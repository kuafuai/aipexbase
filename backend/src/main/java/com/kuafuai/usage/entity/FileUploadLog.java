package com.kuafuai.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件上传日志. 每次 CommonController.uploadFile / uploadByUrl 成功后追加一条.
 * URL 唯一, 重复上传同一 URL 会被 uk_url 挡掉.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_upload_log")
public class FileUploadLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;

    private String fileUrl;

    private Long fileSize;

    private String contentType;

    private String originalName;

    private LocalDateTime uploadedAt;
}
