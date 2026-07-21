package com.kuafuai.usage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.usage.entity.FileUploadLog;
import com.kuafuai.usage.mapper.FileUploadLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 记录文件上传, 供存储用量统计使用.
 * 幂等: 同一 URL 已存在则跳过.
 * 上传失败不能影响业务流程, 调用方要 try/catch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadLogService {

    private final FileUploadLogMapper fileUploadLogMapper;

    public void recordUpload(String appId, String fileUrl, long fileSize,
                             String contentType, String originalName) {
        if (appId == null || fileUrl == null) {
            return;
        }
        // 幂等检查, 避免 uk_url 冲突刷 error 日志
        Long existing = fileUploadLogMapper.selectCount(
                new LambdaQueryWrapper<FileUploadLog>().eq(FileUploadLog::getFileUrl, fileUrl));
        if (existing != null && existing > 0) {
            return;
        }
        FileUploadLog row = FileUploadLog.builder()
                .appId(appId)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .contentType(contentType)
                .originalName(originalName)
                .uploadedAt(LocalDateTime.now())
                .build();
        fileUploadLogMapper.insert(row);
    }
}
