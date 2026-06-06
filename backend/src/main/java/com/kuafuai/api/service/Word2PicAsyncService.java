package com.kuafuai.api.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.file.ImageUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Word2PicAsyncService {

    public static final String CACHE_PREFIX = "word2pic:task:";
    public static final int TTL_MINUTES = 10;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    @Autowired
    private ApiBusinessService apiBusinessService;
    @Resource
    private StorageService storageService;
    @Resource
    private Cache cache;

    // @Async 必须走 Spring 代理，self 注入用来在同类内触发异步
    @Lazy
    @Resource
    private Word2PicAsyncService self;

    private final Gson gson = new Gson();
    private final Type returnValueType = new TypeToken<Map<String, Object>>() {
    }.getType();

    /**
     * Controller 入口：根据 async 参数决定同步或异步。
     */
    public Object handle(Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("login.register.params", "text");
        }

        String appId = GlobalAppIdFilter.getAppId();

        if (isAsync(data)) {
            data.remove("async");
            String taskId = UUID.randomUUID().toString().replace("-", "");
            savePending(taskId);
            self.process(appId, taskId, data);

            Map<String, Object> resp = Maps.newHashMap();
            resp.put("taskId", taskId);
            return ResultUtils.success(resp);
        }

        Object out = processSync(appId, data);
        if (out instanceof String) {
            return ResultUtils.success(out);
        }
        // dataPath 未配置时，保持原行为返回上游原始 JSON Map
        return out;
    }

    /**
     * 查询异步任务结果。
     */
    public Object getTaskResult(String taskId) {
        Map<String, Object> task = cache.getCacheObject(CACHE_PREFIX + taskId);
        if (task == null) {
            return ResultUtils.error("任务不存在或已过期");
        }
        return ResultUtils.success(task);
    }

    private boolean isAsync(Map<String, Object> data) {
        Object v = data.get("async");
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return "true".equalsIgnoreCase(String.valueOf(v));
    }

    /**
     * 同步执行：返回 URL 字符串，或在未配置 dataPath 时返回上游原始 JSON Map。
     */
    public Object processSync(String appId, Map<String, Object> data) {
        try {
            return doSmartImage(appId, data);
        } catch (Exception e) {
            log.warn("=====智能图像API失败，降级到word2pic_old:{}=====", e.getMessage());
            try {
                return callWord2PicOld(appId, data);
            } catch (Exception fallbackException) {
                log.error("=====word2pic_old降级也失败:{}=====", fallbackException.getMessage());
                throw new BusinessException(e.getMessage() + "\n降级失败: " + fallbackException.getMessage());
            }
        }
    }

    /**
     * 异步执行：结果写入缓存，状态 PENDING / SUCCESS / FAIL。
     */
    @Async("word2PicExecutor")
    public void process(String appId, String taskId, Map<String, Object> data) {
        withAppDb(appId, () -> {
            try {
                log.info("thread============{}", Thread.currentThread().getName());
                Object out = doSmartImage(appId, data);
                saveSuccess(taskId, out);
            } catch (Exception e) {
                log.warn("=====智能图像API失败，降级到word2pic_old taskId={}:{}=====", taskId, e.getMessage());
                try {
                    String url = callWord2PicOld(appId, data);
                    saveSuccess(taskId, url);
                } catch (Exception fallbackException) {
                    log.error("=====word2pic_old降级也失败 taskId={}:{}=====", taskId, fallbackException.getMessage());
                    saveFail(taskId, e.getMessage() + "\n降级失败: " + fallbackException.getMessage());
                }
            }
        });
    }

    /**
     * 异步线程不会继承请求线程的数据源 ThreadLocal，按 appId 手动切换。
     */
    private void withAppDb(String appId, Runnable action) {
        String rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(appId, "app");
        try {
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);
            action.run();
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    private Object doSmartImage(String appId, Map<String, Object> data) {
        String apiKey = data.containsKey("files") ? "image_edit" : "image_generation";
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "API配置不存在: " + apiKey);
        }
        String result = apiBusinessService.callApiWithBilling(appId, setting, data);
        return extractAndUpload(appId, setting, result);
    }

    private Object extractAndUpload(String appId, DynamicApiSetting setting, String result) {
        String dataPath = setting.getDataPath();
        if (StringUtils.isEmpty(dataPath)) {
            return gson.fromJson(result, returnValueType);
        }
        Object content = JsonPath.read(result, dataPath);
        String base64String = (String) content;
        if (base64String.startsWith("data:")) {
            int commaIndex = base64String.indexOf(",");
            if (commaIndex > 0) {
                base64String = base64String.substring(commaIndex + 1);
            }
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64String);
        return storageService.upload(imageBytes, appId, "png", "image/png");
    }

    private String callWord2PicOld(String appId, Map<String, Object> data) {
        String apiKey = "word2pic";
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "word2pic API配置不存在");
        }

        List<Map<String, Object>> contents = Lists.newArrayList();
        addText(contents, data);
        addImages(contents, data);

        Map<String, Object> contentMap = Maps.newHashMap();
        contentMap.put("content", contents);

        String result = apiBusinessService.callApiWithBilling(appId, setting, contentMap);
        String dataPath = setting.getDataPath();
        Object content = JsonPath.read(result, dataPath);

        byte[] imageBytes = Base64.getDecoder().decode((String) content);
        return storageService.upload(imageBytes, appId, "png", "image/png");
    }

    private void addText(List<Map<String, Object>> contents, Map<String, Object> data) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("text", data.get("text"));
        contents.add(map);
    }

    private void addImages(List<Map<String, Object>> contents, Map<String, Object> data) {
        Object fileObj = data.get("file");
        Object filesObj = data.get("files");

        List<String> fileList = Lists.newArrayList();

        if (fileObj instanceof String && StringUtils.isNotEmpty((String) fileObj)) {
            fileList.add((String) fileObj);
        }

        if (filesObj instanceof List) {
            List<?> filesList = (List<?>) filesObj;
            for (Object file : filesList) {
                if (file instanceof String && StringUtils.isNotEmpty((String) file)) {
                    fileList.add((String) file);
                }
            }
        }

        for (String file : fileList) {
            byte[] imageBytes = ImageUtils.readFile(file);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> imageMap = Maps.newHashMap();
            imageMap.put("mime_type", "image/png");
            imageMap.put("data", base64);

            Map<String, Object> inlineMap = Maps.newHashMap();
            inlineMap.put("inline_data", imageMap);

            contents.add(inlineMap);
        }
    }

    private void savePending(String taskId) {
        Map<String, Object> task = Maps.newHashMap();
        task.put("status", STATUS_PENDING);
        cache.setCacheObject(CACHE_PREFIX + taskId, task, TTL_MINUTES, TimeUnit.MINUTES);
    }

    private void saveSuccess(String taskId, Object result) {
        Map<String, Object> task = Maps.newHashMap();
        task.put("status", STATUS_SUCCESS);
        task.put("result", result);
        cache.setCacheObject(CACHE_PREFIX + taskId, task, TTL_MINUTES, TimeUnit.MINUTES);
    }

    private void saveFail(String taskId, String message) {
        Map<String, Object> task = Maps.newHashMap();
        task.put("status", STATUS_FAIL);
        task.put("message", message);
        cache.setCacheObject(CACHE_PREFIX + taskId, task, TTL_MINUTES, TimeUnit.MINUTES);
    }
}
