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
import com.kuafuai.common.file.FileUtils;
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


    @PostMapping("/word2pic_old")
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
        addImages(contents, data);

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

    /**
     * ElevenLabs 文字转语音 - 返回二进制音频，上传OSS后返回URL
     */
    @PostMapping("/elevenLabsTTS")
    public Object elevenLabsTTS(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("login.register.params", "text");
        }

        String appId = GlobalAppIdFilter.getAppId();
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, "elevenLabsTTS");
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 调用 ElevenLabs API，获取二进制音频
        byte[] audioBytes;
        if (setting.getMarketId() > 0) {
            audioBytes = apiBusinessService.callApiWithBillingBytes(appId, setting, data);
        } else {
            audioBytes = apiBusinessService.callHttpApiBytes(setting, data, null);
        }

        try {
            // 根据 output_format 参数确定文件格式和 MIME 类型
            String outputFormat = (String) data.getOrDefault("output_format", "mp3_44100_128");
            String fileExt;
            String mimeType;
            if (outputFormat.startsWith("pcm_")) {
                fileExt = "pcm";
                mimeType = "audio/pcm";
            } else if (outputFormat.startsWith("ulaw_")) {
                fileExt = "ulaw";
                mimeType = "audio/basic";
            } else {
                fileExt = "mp3";
                mimeType = "audio/mpeg";
            }

            // 上传到 OSS
            String path = storageService.upload(audioBytes, appId, fileExt, mimeType);
            return ResultUtils.success(path);

        } catch (Exception e) {
            log.error("=====ElevenLabs TTS失败:{}=====", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }


    @PostMapping("/text2music")
    public Object text2music(@RequestBody Map<String, Object> data) {
        String apiKey = "text2music";
        if (!data.containsKey("prompt")) {
            return ResultUtils.error("login.register.params", "prompt");
        }

        String appId = GlobalAppIdFilter.getAppId();
        DynamicApiSetting setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 构建请求参数
        Map<String, Object> contentMap = Maps.newHashMap();
        contentMap.put("model", data.getOrDefault("model", "music-2.5+"));
        contentMap.put("prompt", data.get("prompt"));

        // 可选参数
        if (data.containsKey("lyrics")) {
            contentMap.put("lyrics", data.get("lyrics"));
        }
        if (data.containsKey("duration")) {
            contentMap.put("duration", data.get("duration"));
        }
        if (data.containsKey("style")) {
            contentMap.put("style", data.get("style"));
        }

        // 音频设置
        if (data.containsKey("audio_setting")) {
            contentMap.put("audio_setting", data.get("audio_setting"));
        } else {
            Map<String, Object> audioSetting = Maps.newHashMap();
            audioSetting.put("sample_rate", 44100);
            audioSetting.put("bitrate", 256000);
            audioSetting.put("format", "mp3");
            contentMap.put("audio_setting", audioSetting);
        }

        String result = apiBusinessService.callApiWithBilling(appId, setting, contentMap);
        try {
            String dataPath = setting.getDataPath();
            Object content = JsonPath.read(result, dataPath);

            // 假设返回的是 hex 编码的音频数据
            String hexData = (String) content;
            byte[] audioBytes = FileUtils.hexToBytes(hexData);

            String fileName = (String) data.get("file_name"); // 可选参数
            String path;
            if (StringUtils.isNotBlank(fileName)) {
                path = storageService.upload(audioBytes, GlobalAppIdFilter.getAppId(), fileName, "audio/mpeg");
            } else {
                path = storageService.upload(audioBytes, GlobalAppIdFilter.getAppId(), "mp3", "audio/mpeg");
            }

            return ResultUtils.success(path);

        } catch (Exception e) {
            log.error("=====音乐生成失败:{}=====", e.getMessage() + "\n" + result);
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 智能图像生成/编辑 API
     * - 有 image 参数 → 调用图像编辑 API (multipart)
     * - 无 image 参数 → 调用图像生成 API (json)
     */
    @PostMapping("/word2pic")
    public Object smartImage(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("text")) {
            return ResultUtils.error("login.register.params", "text");
        }

        String appId = GlobalAppIdFilter.getAppId();
        String apiKey;
        DynamicApiSetting setting;

        // 判断是图像编辑还是图像生成
        if (data.containsKey("file")) {
            // 图像编辑模式
            apiKey = "image_edit";
            data.put("model", "gpt-image-2");

            data.put("prompt", data.get("text"));
            data.put("image", data.get("file"));
            data.remove("file");
            data.remove("text");
        } else {
            // 图像生成模式
            apiKey = "image_generation";
        }

        setting = apiBusinessService.getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "API配置不存在: " + apiKey);
        }

        // 调用 API（走计费流程）
        String result = apiBusinessService.callApiWithBilling(appId, setting, data);

        // 解析结果
        try {
            String dataPath = setting.getDataPath();
            if (StringUtils.isNotEmpty(dataPath)) {
                Object content = JsonPath.read(result, dataPath);
                return ResultUtils.success(content);
            } else {
                return gson.fromJson(result, return_value_type);
            }
        } catch (Exception e) {
            log.error("=====智能图像API失败:{}=====", e.getMessage() + "\n" + result);
            throw new BusinessException(e.getMessage() + "\n" + result);
        }
    }

    private void addText(List<Map<String, Object>> contents, Map<String, Object> data) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("text", data.get("text"));
        contents.add(map);
    }

    private void addImages(List<Map<String, Object>> contents, Map<String, Object> data) {
        // 支持单个 file 或多个 files 数组
        Object fileObj = data.get("file");
        Object filesObj = data.get("files");

        List<String> fileList = Lists.newArrayList();

        // 处理单个 file 的情况
        if (fileObj instanceof String && StringUtils.isNotEmpty((String) fileObj)) {
            fileList.add((String) fileObj);
        }

        // 处理 files 数组的情况
        if (filesObj instanceof List) {
            List<?> filesList = (List<?>) filesObj;
            for (Object file : filesList) {
                if (file instanceof String && StringUtils.isNotEmpty((String) file)) {
                    fileList.add((String) file);
                }
            }
        }

        // 将所有图片添加到 contents
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

}
