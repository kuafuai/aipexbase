package com.kuafuai.common.storage.impl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.kuafuai.common.file.FileUploadUtils;
import com.kuafuai.common.file.ImageUtils;
import com.kuafuai.common.file.MimeTypeUtils;
import com.kuafuai.common.storage.StorageService;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;


@Slf4j
@Service("S3StorageServiceImpl")
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageServiceImpl implements StorageService {

    @Value("${storage.s3.access_key:1}")
    private String accessKey;

    @Value("${storage.s3.secret_key:1}")
    private String secretKey;

    @Value("${storage.s3.endpoint:1}")
    private String endpoint;

    @Value("${storage.s3.domain:1}")
    private String domain;

    @Value("${storage.s3.region:1}")
    private String region;

    @Value("${storage.s3.bucket_name:1}")
    private String bucketName;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        log.info("===========================init s3 : {}, {} ======================", accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region)) // 华为区域
                .withClientConfiguration(new ClientConfiguration()
                        .withSignerOverride("S3SignerType")) // 重要！华为 OBS 使用 V2 签名
                .build();
    }


    @Override
    public String upload(MultipartFile file) {
        try {
            FileUploadUtils.assertAllowed(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
            String appId = GlobalAppIdFilter.getAppId();
            if (StringUtils.isEmpty(appId)) {
                appId = "1";
            }
            String fileName = "profile/" + appId + "/" + FileUploadUtils.extractFilename(file);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(file.getContentType());
            objectMetadata.setContentLength(file.getSize());
            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(bucketName, fileName, inputStream, objectMetadata);
            }

            return domain + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String upload(String fileUrl, String formatter, String contentType) {
        String appId = GlobalAppIdFilter.getAppId();
        if (StringUtils.isEmpty(appId)) {
            appId = "1";
        }

        byte[] buffer = ImageUtils.readFile(fileUrl);

        return upload(buffer, appId, formatter, contentType);
    }

    @Override
    public String upload(byte[] buffer, String appId, String formatter, String contentType) {
        try {
            String fileName = "profile/" + appId + "/" + FileUploadUtils.extractFilename(formatter);

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            objectMetadata.setContentLength(buffer.length);

            try (InputStream inputStream = new ByteArrayInputStream(buffer)) {
                s3Client.putObject(bucketName, fileName, inputStream, objectMetadata);
            }

            return domain + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
