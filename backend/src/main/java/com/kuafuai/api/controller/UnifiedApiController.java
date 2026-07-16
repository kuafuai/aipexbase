package com.kuafuai.api.controller;


import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.service.ApiBusinessService;
import com.kuafuai.api.service.Word2PicAsyncService;
import com.kuafuai.api.service.AiAnalysisService;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.file.FileUtils;
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
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class UnifiedApiController {

    @Autowired
    private ApiBusinessService apiBusinessService;
    @Resource
    private StorageService storageService;
    @Resource
    private Word2PicAsyncService word2PicAsyncService;
    @Resource
    private AiAnalysisService aiAnalysisService;

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
     * - 有 files 参数 → 调用图像编辑 API；否则调用图像生成 API
     * - 失败时降级到 word2pic_old 方式
     * - 请求里携带 async=true 时走异步：立即返回 taskId，前端通过
     * GET /api/word2pic/result/{taskId} 轮询结果，缓存保留 10 分钟。
     */
    @PostMapping("/word2pic")
    public Object smartImage(@RequestBody Map<String, Object> data) {
        return word2PicAsyncService.handle(data);
    }

    /**
     * 查询 word2pic 异步任务结果
     * 返回字段：status = PENDING / SUCCESS / FAIL；成功带 result，失败带 message
     */
    @GetMapping("/word2pic/result/{taskId}")
    public Object word2picResult(@PathVariable("taskId") String taskId) {
        return word2PicAsyncService.getTaskResult(taskId);
    }

    /**
     * AI 图文理解（去 Dify 版）
     * 入参：prompt(职责) / query(用户需求) / files(图片 url 或 base64 data uri，支持数组) / conversation_id / model(可选)
     * 返回：{answer, conversation_id, metadata.usage} —— 与原 Dify 结构一致，前端读 .answer
     */
    @PostMapping("/AiAnalysis")
    public Object aiAnalysis(@RequestBody Map<String, Object> data) {
        data.put("ip", ServletUtils.getClientIp());
        return aiAnalysisService.analyze(GlobalAppIdFilter.getAppId(), data);
    }

}
