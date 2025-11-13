package com.kuafuai.common.storage.impl;


import com.kuafuai.common.config.AppConfig;
import com.kuafuai.common.file.FileUploadUtils;
import com.kuafuai.common.storage.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service("LocalStorageServiceImpl")
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageServiceImpl implements StorageService {
    @Override
    public String upload(MultipartFile file) {
        try {

            String filePath = AppConfig.getUploadPath();
            return FileUploadUtils.upload(filePath, file);

        } catch (IOException e) {
            throw new RuntimeException("文件上传报错");
        }
    }

    @Override
    public String upload(String fileUrl, String formatter, String contentType) {
        return null;
    }

    @Override
    public String upload(byte[] buffer, String appId, String formatter, String contentType) {
        return null;
    }
}
