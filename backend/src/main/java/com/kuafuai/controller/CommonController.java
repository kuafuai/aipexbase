package com.kuafuai.controller;


import cn.hutool.http.HttpUtil;
import com.google.common.collect.Maps;
import com.kuafuai.common.config.DeployConfig;
import com.kuafuai.common.config.MessageConfig;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.file.FileUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import cn.hutool.core.util.ZipUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
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

    @Resource
    private AppInfoService appInfoService;

    @Resource
    private DeployConfig deployConfig;

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

    @PostMapping("/deploy")
    public BaseResponse deploy(MultipartFile file, @RequestParam String appId) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
                return ResultUtils.error("仅支持上传zip文件");
            }

            // 检查文件大小限制（转换为字节）
            long maxFileSizeBytes = deployConfig.getMaxFileSize() * 1024 * 1024;
            if (file.getSize() > maxFileSizeBytes) {
                return ResultUtils.error("文件大小超过限制，最大支持" + deployConfig.getMaxFileSize() + "MB");
            }

            // 通过 appId 查询数据库获取 id
            AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
            if (appInfo == null || appInfo.getId() == null) {
                return ResultUtils.error("应用不存在");
            }
            Long id = appInfo.getId();

            // 使用配置化的目标目录
            String targetDir = deployConfig.getWorkspaceDir() + "/" + id + "/1" + id + "/";

            // 解压 zip 到目标目录
            File targetPath = new File(targetDir);
            if (!targetPath.exists()) {
                targetPath.mkdirs();
            }
            ZipUtil.unzip(file.getInputStream(), targetPath, StandardCharsets.UTF_8);

            // 使用配置化的部署URL
            String deployUrl = deployConfig.getBaseUrl() + "/1" + id;
            Map<String, String> data = Maps.newHashMap();
            data.put("url", deployUrl);
            return ResultUtils.success(data);
        } catch (Exception e) {
            return ResultUtils.error(e.getMessage());
        }
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
