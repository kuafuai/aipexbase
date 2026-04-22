package com.kuafuai.api.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class MultipartBuilder {

    private final OkHttpClient httpClient;

    public MultipartBuilder(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 构建 Multipart 请求体（支持 URL / 多图 / 普通参数）
     */
    public RequestBody buildMultipartBody(Map<String, Object> params) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) continue;

            // 跳过特殊字段
            if (isIgnoreKey(key)) continue;

            //多值支持（image[]）
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    handleValue(builder, key + "[]", item);
                }
            } else {
                handleValue(builder, key, value);
            }
        }

        return builder.build();
    }

    /**
     * 处理单个字段
     */
    private void handleValue(MultipartBody.Builder builder, String key, Object value) {
        if (value instanceof String && isUrl((String) value)) {
            addFileFromUrl(builder, key, (String) value);
        } else {
            builder.addFormDataPart(key, value.toString());
            log.info("Added form part: {}={}", key, value);
        }
    }

    /**
     * URL → 下载 → 转 multipart file（流式）
     */
    private void addFileFromUrl(MultipartBody.Builder builder, String key, String url) {
        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                log.error("下载图片失败: url={}, code={}", url, response.code());
                throw new RuntimeException("下载失败: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new RuntimeException("响应体为空");
            }

            String contentType = response.header("Content-Type");
            if (contentType != null && contentType.contains(";")) {
                contentType = contentType.split(";")[0];
            }

            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }

            MediaType mediaType = MediaType.parse(contentType);

            byte[] imageBytes = body.bytes();

            // 创建 RequestBody
            RequestBody fileBody = RequestBody.create(mediaType, imageBytes);

            // 文件名（避免重复 + 提升兼容性）
            String extension = guessExtension(contentType);
            String fileName = UUID.randomUUID() + "." + extension;

            builder.addFormDataPart(key, fileName, fileBody);

            log.info("Added file part from URL: key={}, url={}, size={} bytes, type={}, fileName={}", key, url, imageBytes.length, contentType, fileName);

        } catch (Exception e) {
            log.error("处理图片URL失败: url={}, error={}", url, e.getMessage());
            throw new RuntimeException("处理图片URL失败: " + url, e);
        }
    }

    /**
     * 判断是否 URL
     */
    private boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /**
     * 忽略字段
     */
    private boolean isIgnoreKey(String key) {
        return "token".equalsIgnoreCase(key) || "ip".equalsIgnoreCase(key);
    }

    /**
     * 根据 Content-Type 推断扩展名
     */
    private String guessExtension(String contentType) {
        if (contentType == null) return "bin";

        switch (contentType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/bmp":
                return "bmp";
            default:
                return "bin";
        }
    }
}
