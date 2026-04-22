package com.kuafuai.api.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.api.util.ApiUtil;
import com.kuafuai.common.util.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ApiClient {

    private final OkHttpClient httpClient;
    private final MultipartBuilder multipartBuilder;
    private final Gson gson = new Gson();

    private final Type header_value_type = new TypeToken<Map<String, String>>() {
    }.getType();
    private final Type return_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    public ApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(50, TimeUnit.MINUTES)
                .build();
        this.multipartBuilder = new MultipartBuilder(httpClient);
    }

    /**
     * 构建模板参数映射
     */
    private Map<String, String> buildTemplateMaps(Map<String, Object> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                String raw = (String) value;
                                if (raw.trim().startsWith("{") || raw.trim().startsWith("[")) {
                                    return raw;
                                } else {
                                    if (raw.contains("\"") || raw.contains("\\") || raw.contains("\n") || raw.contains("\r") || raw.contains("\t")) {
                                        raw = JSON.toJSONString(value);
                                        if (raw.startsWith("\"") && raw.endsWith("\"")) {
                                            raw = raw.substring(1, raw.length() - 1);
                                        }
                                    }
                                    return raw;
                                }
                            }
                            return gson.toJson(entry.getValue());
                        }
                ));
    }

    /**
     * 构建 OkHttp Request 对象
     */
    private Request buildRequest(ApiDefinition apiDef, Map<String, Object> params, Map<String, String> templateMaps) {
        String urlWithParams = ApiUtil.interpolateString(apiDef.url, templateMaps);
        Request.Builder requestBuilder;

        if ("GET".equalsIgnoreCase(apiDef.method)) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(urlWithParams).newBuilder();
            for (Map.Entry<String, String> e : templateMaps.entrySet()) {
                if (!StringUtils.equalsAnyIgnoreCase(e.getKey(), "token", "ip")) {
                    urlBuilder.addQueryParameter(e.getKey(), e.getValue());
                }
            }
            requestBuilder = new Request.Builder().url(urlBuilder.build()).get();
        } else if ("POST".equalsIgnoreCase(apiDef.method)) {
            RequestBody body;
            if ("template".equalsIgnoreCase(apiDef.bodyType)) {
                String rendered = ApiUtil.interpolateString(apiDef.bodyTemplate, templateMaps);
                log.info("Rendered template: {}", rendered);
                body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), rendered);
            } else if ("form".equalsIgnoreCase(apiDef.bodyType)) {
                String rendered = ApiUtil.interpolateString(apiDef.bodyTemplate, templateMaps);
                log.info("Rendered form body: {}", rendered);
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (String pair : rendered.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        formBuilder.add(kv[0].trim(), kv[1].trim());
                    }
                }
                body = formBuilder.build();
            } else if ("multipart".equalsIgnoreCase(apiDef.bodyType)) {
                body = buildMultipartBody(params);
            } else {
                body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        gson.toJson(params)
                );
            }
            requestBuilder = new Request.Builder().url(urlWithParams).post(body);
        } else {
            throw new UnsupportedOperationException("Method not supported: " + apiDef.method);
        }

        // 处理header
        if (StringUtils.isNotEmpty(apiDef.headers)) {
            Map<String, String> headerMap = gson.fromJson(apiDef.headers, header_value_type);
            for (Map.Entry<String, String> e : headerMap.entrySet()) {
                requestBuilder.addHeader(e.getKey(), ApiUtil.interpolateString(e.getValue(), templateMaps));
            }
        }

        return requestBuilder.build();
    }

    /**
     * 调用API并返回字符串响应（JSON等文本响应）
     */
    public String call(ApiDefinition apiDef, Map<String, Object> params) {
        Map<String, String> templateMaps = buildTemplateMaps(params);
        Request request = buildRequest(apiDef, params, templateMaps);

        logRequest(request, apiDef, templateMaps);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            logResponse(response, responseBody);
            return responseBody;
        } catch (IOException e) {
            log.error("API请求失败: url={}, method={}, error={}", apiDef.url, apiDef.method, e.getMessage());
            throw new RuntimeException("API请求失败", e);
        }
    }

    /**
     * 调用API并返回二进制字节数组（用于音频、图片等二进制响应）
     */
    public byte[] callBytes(ApiDefinition apiDef, Map<String, Object> params) {
        Map<String, String> templateMaps = buildTemplateMaps(params);
        Request request = buildRequest(apiDef, params, templateMaps);

        logRequest(request, apiDef, templateMaps);

        try (Response response = httpClient.newCall(request).execute()) {
            byte[] responseBytes = response.body().bytes();
            log.info("============================================ API Response (binary) ==========");
            log.info("Status Code: {}", response.code());
            log.info("Content-Type: {}", response.header("Content-Type"));
            log.info("Response Body Size: {} bytes", responseBytes.length);
            log.info("====================================================================");

            // 如果状态码不是200，尝试解析错误信息
            if (response.code() != 200) {
                String errorBody = new String(responseBytes, "UTF-8");
                log.error("API返回非200状态码: {}, body: {}", response.code(), errorBody);
                throw new RuntimeException("API返回错误: " + response.code() + ", " + errorBody);
            }

            return responseBytes;
        } catch (IOException e) {
            log.error("API请求失败(binary): url={}, method={}, error={}", apiDef.url, apiDef.method, e.getMessage());
            throw new RuntimeException("API请求失败", e);
        }
    }

    /**
     * 打印请求日志
     */
    private void logRequest(Request request, ApiDefinition apiDef, Map<String, String> params) {
        try {
            log.info("============================================ API Request ==========");
            log.info("URL: {}", request.url());
            log.info("Method: {}", request.method());

            if (!request.headers().names().isEmpty()) {
                log.info("Headers: ");
                for (String name : request.headers().names()) {
                    log.info("  {}: {}", name, request.headers().get(name));
                }
            }

            if (request.body() != null) {
                if ("template".equalsIgnoreCase(apiDef.bodyType)) {
                    String rendered = ApiUtil.interpolateString(apiDef.bodyTemplate, params);
                    log.info("Request Body (template): {}", rendered);
                } else {
                    log.info("Request Body (json): {}", gson.toJson(params));
                }
            }
        } catch (Exception e) {
            log.warn("打印请求日志失败: {}", e.getMessage());
        }
    }

    /**
     * 打印响应日志
     */
    private void logResponse(Response response, String responseBody) {
        try {
            log.info("============================================ API Response ==========");
            log.info("Status Code: {}", response.code());
            log.info("Response Body: {}", responseBody != null && responseBody.length() > 1000
                    ? responseBody.substring(0, 1000) + "... (truncated)"
                    : responseBody);
            log.info("====================================================================");
        } catch (Exception e) {
            log.warn("打印响应日志失败: {}", e.getMessage());
        }
    }

    /**
     * 构建 Multipart 请求体（委托给 MultipartBuilder）
     */
    private RequestBody buildMultipartBody(Map<String, Object> params) {
        return multipartBuilder.buildMultipartBody(params);
    }

}
