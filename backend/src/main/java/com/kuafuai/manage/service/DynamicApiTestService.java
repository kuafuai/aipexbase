package com.kuafuai.manage.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.api.util.ApiUtil;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.JSON;
import com.kuafuai.manage.entity.vo.DynamicApiTestResultVO;
import com.kuafuai.manage.entity.vo.DynamicApiTestVo;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.DynamicApiSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Executes a single test call against a stored dynamic API. Reuses the same
 * template / body / header semantics as the production runtime
 * ({@link com.kuafuai.api.client.ApiClient}) but captures the full request +
 * response for display in the dashboard debug panel.
 *
 * Non-2xx responses are NOT errors here — the point of the test is to reveal
 * what the target API returned. Only true transport failures (DNS, timeout,
 * connection refused) populate {@code error}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicApiTestService {

    private final AppInfoService appInfoService;
    private final DynamicApiSettingService dynamicApiSettingService;

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();
    private final Type stringMapType = new TypeToken<Map<String, String>>() {}.getType();

    public DynamicApiTestResultVO test(String appId, String keyName, DynamicApiTestVo testVo) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        LambdaQueryWrapper<DynamicApiSetting> qw = new LambdaQueryWrapper<>();
        qw.eq(DynamicApiSetting::getAppId, appId);
        qw.eq(DynamicApiSetting::getKeyName, keyName);
        DynamicApiSetting api = dynamicApiSettingService.getOne(qw);
        if (api == null) {
            throw new BusinessException("error.code.not_found");
        }

        ApiDefinition def = ApiDefinition.builder()
                .name(api.getKeyName())
                .method(StringUtils.defaultString(api.getMethod(), "GET"))
                .url(StringUtils.defaultString(testVo.getOverrideUrl(), api.getUrl()))
                .headers(api.getHeader())
                .bodyType(StringUtils.defaultString(api.getBodyType(), "template"))
                .bodyTemplate(StringUtils.defaultString(api.getBodyTemplate(), ""))
                .build();

        return runOne(def, testVo);
    }

    /**
     * Test an inline (unsaved) API definition. Only requires appId for auth
     * check — the request config comes entirely from {@code testVo}.
     */
    public DynamicApiTestResultVO testInline(String appId, DynamicApiTestVo testVo) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }
        if (StringUtils.isEmpty(testVo.getUrl())) {
            throw new BusinessException("error.code.params_error");
        }

        ApiDefinition def = ApiDefinition.builder()
                .name("inline-test")
                .method(StringUtils.defaultString(testVo.getMethod(), "GET"))
                .url(StringUtils.defaultString(testVo.getOverrideUrl(), testVo.getUrl()))
                .headers(testVo.getHeaderTemplate())
                .bodyType(StringUtils.defaultString(testVo.getBodyType(), "template"))
                .bodyTemplate(StringUtils.defaultString(testVo.getBodyTemplate(), ""))
                .build();

        return runOne(def, testVo);
    }

    /**
     * Shared execution path — dispatch the built definition, capture the
     * full request/response cycle, and return a result VO.
     */
    private DynamicApiTestResultVO runOne(ApiDefinition def, DynamicApiTestVo testVo) {
        Map<String, Object> params = testVo.getParams() != null
                ? testVo.getParams()
                : Collections.emptyMap();
        Map<String, String> templateMap = buildTemplateMap(params);

        Request request;
        String requestBodyForDisplay;
        try {
            RequestBuild built = buildRequest(def, params, templateMap, testVo.getOverrideHeaders());
            request = built.request;
            requestBodyForDisplay = built.renderedBody;
        } catch (Exception e) {
            log.warn("Failed to build test request for {}: {}", def.getName(), e.getMessage());
            return DynamicApiTestResultVO.builder()
                    .requestUrl(def.getUrl())
                    .requestMethod(def.getMethod())
                    .error("请求构造失败: " + e.getMessage())
                    .build();
        }

        long startedAt = System.currentTimeMillis();
        try (Response response = http.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - startedAt;
            String body = response.body() != null ? response.body().string() : "";
            long sizeBytes = body != null ? body.getBytes().length : 0L;

            Object bodyJson = null;
            if (StringUtils.isNotEmpty(body)) {
                String trimmed = body.trim();
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    try {
                        bodyJson = gson.fromJson(body, Object.class);
                    } catch (JsonSyntaxException ignored) {
                        // not JSON, leave null
                    }
                }
            }

            return DynamicApiTestResultVO.builder()
                    .requestUrl(request.url().toString())
                    .requestMethod(request.method())
                    .requestHeaders(headersToMap(request.headers()))
                    .requestBody(requestBodyForDisplay)
                    .status(response.code())
                    .statusText(response.message())
                    .responseHeaders(headersToMap(response.headers()))
                    .body(body)
                    .bodyJson(bodyJson)
                    .timeMs(elapsed)
                    .sizeBytes(sizeBytes)
                    .build();
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("API test transport-level failure for {}: {}", def.getName(), e.getMessage());
            return DynamicApiTestResultVO.builder()
                    .requestUrl(request.url().toString())
                    .requestMethod(request.method())
                    .requestHeaders(headersToMap(request.headers()))
                    .requestBody(requestBodyForDisplay)
                    .timeMs(elapsed)
                    .error(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }

    // ---------- Helpers ----------

    private Map<String, String> buildTemplateMap(Map<String, Object> params) {
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Object v = e.getValue();
                            if (v instanceof String) return (String) v;
                            return gson.toJson(v);
                        }
                ));
    }

    private static class RequestBuild {
        final Request request;
        final String renderedBody;
        RequestBuild(Request req, String rendered) {
            this.request = req;
            this.renderedBody = rendered;
        }
    }

    private RequestBuild buildRequest(
            ApiDefinition def,
            Map<String, Object> params,
            Map<String, String> templateMap,
            Map<String, String> overrideHeaders
    ) {
        String urlRendered = ApiUtil.interpolateString(def.getUrl(), templateMap);
        Request.Builder rb;
        String renderedBody = null;

        if ("GET".equalsIgnoreCase(def.getMethod())) {
            HttpUrl parsed = HttpUrl.parse(urlRendered);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid URL: " + urlRendered);
            }
            HttpUrl.Builder ub = parsed.newBuilder();
            for (Map.Entry<String, String> e : templateMap.entrySet()) {
                if (!StringUtils.equalsAnyIgnoreCase(e.getKey(), "token", "ip")) {
                    ub.addQueryParameter(e.getKey(), e.getValue());
                }
            }
            rb = new Request.Builder().url(ub.build()).get();
        } else {
            RequestBody body;
            if ("form".equalsIgnoreCase(def.getBodyType())) {
                renderedBody = ApiUtil.interpolateString(def.getBodyTemplate(), templateMap);
                FormBody.Builder fb = new FormBody.Builder();
                for (String pair : renderedBody.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) fb.add(kv[0].trim(), kv[1].trim());
                }
                body = fb.build();
            } else if ("template".equalsIgnoreCase(def.getBodyType())) {
                renderedBody = ApiUtil.interpolateString(def.getBodyTemplate(), templateMap);
                body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        renderedBody);
            } else {
                renderedBody = gson.toJson(params);
                body = RequestBody.create(
                        MediaType.get("application/json; charset=utf-8"),
                        renderedBody);
            }
            rb = new Request.Builder().url(urlRendered).method(def.getMethod().toUpperCase(), body);
        }

        // Merge stored headers + overrides
        Map<String, String> allHeaders = new LinkedHashMap<>();
        if (StringUtils.isNotEmpty(def.getHeaders())) {
            try {
                Map<String, String> stored = gson.fromJson(def.getHeaders(), stringMapType);
                if (stored != null) {
                    for (Map.Entry<String, String> e : stored.entrySet()) {
                        allHeaders.put(e.getKey(), ApiUtil.interpolateString(e.getValue(), templateMap));
                    }
                }
            } catch (JsonSyntaxException ignored) {}
        }
        if (overrideHeaders != null) {
            allHeaders.putAll(overrideHeaders);
        }
        for (Map.Entry<String, String> e : allHeaders.entrySet()) {
            rb.addHeader(e.getKey(), e.getValue());
        }

        return new RequestBuild(rb.build(), renderedBody);
    }

    private Map<String, String> headersToMap(Headers headers) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String name : headers.names()) {
            out.put(name, headers.get(name));
        }
        return out;
    }
}
