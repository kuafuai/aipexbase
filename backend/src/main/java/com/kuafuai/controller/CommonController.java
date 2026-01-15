package com.kuafuai.controller;


import cn.hutool.http.HttpUtil;
import com.google.common.collect.Maps;
import com.kuafuai.common.config.MessageConfig;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.file.FileUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/common", "/admin/common"})
public class CommonController {

    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "mpeg", "3gp");

    @Resource
    private MessageConfig messageConfig;

    @Resource
    private StorageService storageService;

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public BaseResponse uploadFile(MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String fileName = storageService.upload(file);
            if (messageConfig.isEnable()) {
                sendMessage(fileName, messageConfig);
            }
            Map<String, String> data = Maps.newHashMap();
            data.put("url", fileName);
            data.put("fileName", fileName);
            data.put("newFileName", FileUtils.getName(fileName));
            data.put("originalFilename", file.getOriginalFilename());
            return ResultUtils.success(data);
        } catch (Exception e) {
            return ResultUtils.error(e.getMessage());
        }
    }

    @PostMapping("/uploadByUrl")
    public BaseResponse uploadByUrl(@RequestBody Map<String, String> params) {
        String fileUrl = params.get("fileUrl");
        if (StringUtils.isEmpty(fileUrl)) {
            return ResultUtils.error("error.code.params_error");
        }
        String formatter = params.get("formatter");
        if (StringUtils.isEmpty(formatter)) {
            return ResultUtils.error("error.code.params_error");
        }
        String contentType = params.get("contentType");
        if (StringUtils.isEmpty(contentType)) {
            return ResultUtils.error("error.code.params_error");
        }
        String resultUrl = storageService.upload(fileUrl, formatter, contentType);
        return ResultUtils.success(resultUrl);
    }

    /**
     * 发送消息
     *
     * @param fileName
     * @param messageConfig
     */
    private void sendMessage(String fileName, MessageConfig messageConfig) {
        final String appId = GlobalAppIdFilter.getAppId();
        final String notifyUrl = messageConfig.getNotifyUrl();

        // 获取文件扩展名
        String extension = getFileExtension(fileName);
        // 判断扩展名是否在视频格式列表中
        // 视频文件的二进制文件头校验
        if (VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
            final HashMap<String, Object> body = new HashMap<>();
            body.put("msg_type", "text");
            final HashMap<String, Object> contentMap = new HashMap<>();
            contentMap.put("text", "appId:" + appId + ",上传了视频,url 路径为:" + fileName);

            body.put("content", contentMap);
            HttpUtil.post(notifyUrl, JSON.toJSONString(body));
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // 没有扩展名或扩展名为空
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
