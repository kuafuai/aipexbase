package com.kuafuai.api.controller;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.parser.ApiResultParser;
import com.kuafuai.api.service.ApiBusinessService;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.file.ImageUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@Slf4j
public class UnifiedApiController {

    @Autowired
    private ApiBusinessService apiBusinessService;
    @Resource
    private StorageService storageService;

    private final Gson gson = new Gson();

    private final Type return_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    @PostMapping("/{key}")
    public Object handle(
            @PathVariable(value = "key") String apiKey,
            @RequestBody Map<String, Object> data
    ) {
        String appId = GlobalAppIdFilter.getAppId();
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 设置环境变量参数
        data.put("ip", ServletUtils.getClientIp());
        String result = "";
        if (setting.getMarketId() > 0) {
            result = apiBusinessService.callApiWithBilling(appId, setting, data);
        } else {
            result = apiBusinessService.callHttpApi(setting, data, null);
            ApiResultParser.parser(result);
        }
        String dataPath = setting.getDataPath();
        String dataType = setting.getDataType();
        try {
            if (StringUtils.isNotEmpty(dataPath)) {
                Object content = JsonPath.read(result, dataPath);

                if (StringUtils.isNotEmpty(dataType) && StringUtils.equalsIgnoreCase(dataType, "json")) {
                    if (content instanceof String) {
                        String contentStr = (String) content;
                        if (contentStr.trim().startsWith("{") || contentStr.trim().startsWith("[")) {
                            return ResultUtils.success(gson.fromJson(contentStr, return_value_type));
                        } else {
                            return ResultUtils.success(contentStr);
                        }
                    } else {
                        return ResultUtils.success(content);
                    }
                } else {
                    return ResultUtils.success(content);
                }
            } else {
                return gson.fromJson(result, return_value_type);
            }
        } catch (Exception e) {
            throw new BusinessException(e.getMessage() + "\n" + result);
        }
    }


    @PostMapping("/word2pic")
    public Object word2pic(@RequestBody Map<String, Object> data) {
        String apiKey = "word2pic";
        if (!data.containsKey("text")) {
            return ResultUtils.error("login.register.params", "text");
        }

        String appId = GlobalAppIdFilter.getAppId();
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        List<Map<String, Object>> contents = Lists.newArrayList();

        addText(contents, data);
        addImage(contents, data);

        Map<String, Object> contentMap = Maps.newHashMap();
        contentMap.put("content", contents);

        String result = apiBusinessService.callApiWithBilling(appId, setting, contentMap);
        try {
            String dataPath = setting.getDataPath();
            Object content = JsonPath.read(result, dataPath);

            byte[] imageBytes = Base64.getDecoder().decode((String) content);
            String path = storageService.upload(imageBytes, GlobalAppIdFilter.getAppId(), "png", "image/png");
            return ResultUtils.success(path);

        } catch (Exception e) {
            log.error("=====文生图失败:{}=====", e.getMessage() + "\n" + result);
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    private void addText(List<Map<String, Object>> contents, Map<String, Object> data) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("text", data.get("text"));
        contents.add(map);
    }

    private void addImage(List<Map<String, Object>> contents, Map<String, Object> data) {
        String file = StringUtils.trimToEmpty((String) data.get("file"));
        if (StringUtils.isEmpty(file)) {
            return;
        }

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
