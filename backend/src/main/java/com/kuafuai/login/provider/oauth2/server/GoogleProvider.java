package com.kuafuai.login.provider.oauth2.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.login.entity.OAuth2UserInfo;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Google OAuth2提供商实现
 */
@Slf4j
@Component
public class GoogleProvider implements OAuth2ProviderInterface {

    private static final String googleAuthorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String googleTokenGetUrl = "https://oauth2.googleapis.com/token";
    private static final String googleUserInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Resource
    private DynamicConfigBusinessService dynamicConfigBusinessService;


//    WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().proxy(x -> x.type(ProxyProvider.Proxy.SOCKS5).host("127.0.0.1").port(7897)))).build();
    WebClient webClient = WebClient.builder().build();
    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public String getAccessToken(String code) {

        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        if (map.isEmpty()) {
            throw new RuntimeException("Google OAuth2配置不存在");
        }

        Map<String, String> params = new HashMap<>();
        params.put("client_id", map.getOrDefault("google.oauth.client_id", ""));
        params.put("client_secret", map.getOrDefault("google.oauth.client_secret", ""));
        params.put("redirect_uri", map.getOrDefault("google.oauth.redirect_uri", ""));
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        try {
            String response = webClient.post()
                    .uri(googleTokenGetUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(buildFormData(params)).exchangeToMono(resp -> {
                        if (resp.statusCode().isError()) {
                            return resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Error " + resp.statusCode() + ": " + body)));
                        }
                        return resp.bodyToMono(String.class);
                    })
                    .block();

            if (Objects.isNull(response)) {
                return "";
            }

            System.out.println("getAccessToken: " +response);

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            return jsonResponse.get("access_token").getAsString();
        } catch (Exception e) {
            log.error("获取Google访问令牌失败", e);
            throw new RuntimeException("获取访问令牌失败: " + e.getMessage());
        }
    }

    @Override
    public OAuth2UserInfo getUserInfo(String accessToken) {

        try {
            String response = webClient.get()
                    .uri(googleUserInfoUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().isError()) {
                            return resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("Error " + resp.statusCode() + ": " + body)));
                        }
                        return resp.bodyToMono(String.class);
                    })
                    .block();

            System.out.println("getUserInfo: "+response);
            
            // 解析Google返回的用户信息JSON
            JsonObject userJson = JsonParser.parseString(response).getAsJsonObject();
            
            OAuth2UserInfo userInfo = new OAuth2UserInfo();
            // 设置用户ID (Google使用sub字段作为唯一标识)
            userInfo.setId(getStringValue(userJson, "sub"));
            // 设置用户名 (使用email作为用户名)
            userInfo.setUsername(getStringValue(userJson, "email"));
            // 设置邮箱
            userInfo.setEmail(getStringValue(userJson, "email"));
            // 设置头像URL
            userInfo.setAvatar(getStringValue(userJson, "picture"));
            // 设置昵称 (使用name字段)
            userInfo.setNickname(getStringValue(userJson, "name"));
            // 设置原始用户信息
            userInfo.setRawUserInfo(userJson);
            
            return userInfo;
        } catch (Exception e) {
            log.error("获取Google用户信息失败", e);
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);

        if (map.isEmpty()) {
            throw new RuntimeException("Google OAuth2配置不存在");
        }

        Map<String, String> params = new HashMap<>();
        params.put("client_id", map.getOrDefault("google.oauth.client_id", ""));
        params.put("redirect_uri", map.getOrDefault("google.oauth.redirect_uri", ""));
        params.put("response_type", "code");
        params.put("scope", "openid email profile");
        params.put("state", state);

        return buildQueryString(googleAuthorizeUrl, params);
    }

    @Override
    public String getCallbackUri() {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        if (map.isEmpty()) {
            throw new RuntimeException("Google OAuth2配置不存在");
        }

        return map.getOrDefault("google.oauth.callback_uri", "");
    }


    private String buildQueryString(String baseUrl, Map<String, String> params) {
        return getString(baseUrl, params);
    }
    private String buildFormData(Map<String, String> params) {
        return getString("",params);
    }

    /**
     * 安全地从JsonObject中获取字符串值
     * @param jsonObject JSON对象
     * @param key 键名
     * @return 字符串值，如果不存在或为null则返回空字符串
     */
    private String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception e) {
            log.warn("获取JSON字段 {} 失败: {}", key, e.getMessage());
            return "";
        }
    }

    @NotNull
    static String getString(String baseUrl, Map<String, String> params)  {
        StringBuilder queryString;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            queryString = new StringBuilder(baseUrl);
            queryString.append("?");
        }else {
            queryString = new StringBuilder();
        }
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > baseUrl.length() + 1) {
                    queryString.append("&");
                }
                queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            }
        }catch (UnsupportedEncodingException e){
            return "";
        }


        return queryString.toString();
    }
}
