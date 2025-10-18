package com.kuafuai.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(MultipartFile file);

    String upload(byte[] buffer, String appId, String formatter, String contentType);
}
