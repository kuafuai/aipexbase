package com.kuafuai.api.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.api.util.ApiUtil;
import com.kuafuai.common.util.JSON;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ApiClient {

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    private final Type header_value_type = new TypeToken<Map<String, String>>() {
    }.getType();
    private final Type return_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    public ApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();
    }

    public String call(ApiDefinition apiDef, Map<String, Object> params) {

        Map<String, String> templateMaps = params.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Object value = entry.getValue();
                            // 如果是字符串，直接返回
                            if (value instanceof String) {
                                String raw=(String) value;

                                // 检查是否含有需要转义的字符
                                if (raw.contains("\"") || raw.contains("\\") || raw.contains("\n") || raw.contains("\r") || raw.contains("\t")) {
                                    raw = JSON.toJSONString(value); // 会自动转义
                                    // 去掉最外层引号
                                    if (raw.startsWith("\"") && raw.endsWith("\"")) {
                                        raw = raw.substring(1, raw.length() - 1);
                                    }
                                }
                                return raw;
                            }
                            // 如果是 List/Map，用 gson 序列化
                            return gson.toJson(entry.getValue());
                        }
                ));

        // 处理地址
        String urlWithParams = ApiUtil.interpolateString(apiDef.url, templateMaps);
        Request.Builder requestBuilder;

        if ("GET".equalsIgnoreCase(apiDef.method)) {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(urlWithParams).newBuilder();
            for (Map.Entry<String, String> e : templateMaps.entrySet()) {
                urlBuilder.addQueryParameter(e.getKey(), e.getValue());
            }
            requestBuilder = new Request.Builder().url(urlBuilder.build()).get();
        } else if ("POST".equalsIgnoreCase(apiDef.method)) {
            RequestBody body;
            if ("template".equalsIgnoreCase(apiDef.bodyType)) {
                // 通过 模版 转换
                String rendered = ApiUtil.interpolateString(apiDef.bodyTemplate, templateMaps);

                body = RequestBody.create(MediaType.get("application/json; charset=utf-8"), rendered);
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

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("API请求失败", e);
        }
    }


}
