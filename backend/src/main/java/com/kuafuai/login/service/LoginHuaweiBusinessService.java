package com.kuafuai.login.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.config.HuaweiConfig;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 华为元服务账号登录业务服务
 * 通过授权码换取用户级 access_token，再获取华为账号 openID。
 * 认证方式：JWT（PS256）client_assertion
 */
@Service
@Slf4j
public class LoginHuaweiBusinessService {

    @Autowired
    private HuaweiConfig huaweiConfig;

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;

    @Value("${backend.url:}")
    private String backendUrl;

    @Value("${backend.internal.auth-key:b54igLGJ1DpB8OMF}")
    private String backendAuthKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String HW_TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/agent/v1/access-token";
    private static final String HW_USER_INFO_URL = "https://account.cloud.huawei.com/rest.php?nsp_svc=GOpen.User.getInfo";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String JWT_AUD = "https://oauth-login.cloud.huawei.com/oauth2/agent/token";

    /**
     * 用授权码换取华为 openID。
     *
     * @param authCode 华为客户端传来的授权码
     * @return 华为账号 openID
     */
    public String getOpenId(String authCode) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> config = dynamicConfigBusinessService.getSystemConfig(appId);

        // keyId / privateKey 从配置读取，用于 JWT 签名
        String keyId      = config.getOrDefault("huawei.key-id",      huaweiConfig.getKeyId());
        String privateKey = config.getOrDefault("huawei.private-key", huaweiConfig.getPrivateKey());
        log.info("[HW Login] keyId={}, privateKey configured={}", keyId, StringUtils.isNotEmpty(privateKey));

        // projectId / subAccount 从 backend 接口获取，用于 Token 请求 Header 和 JWT iss/sub
        log.info("[HW Login] 从 backend 获取华为应用凭证, appId={}", appId);
        Map<String, String> hwAppCredentials = fetchHwAppCredentials(appId);
        String projectId  = hwAppCredentials.get("projectId");
        String subAccount = hwAppCredentials.get("subAccount");
        log.info("[HW Login] project_id={}, sub_account={}", projectId, subAccount);

        if (StringUtils.isEmpty(projectId) || StringUtils.equalsAnyIgnoreCase(authCode, "codeflying")) {
            log.warn("[HW Login] projectId 为空或 authCode=codeflying，跳过登录, appId={}", appId);
            return "codeflying";
        }

        // 1. 生成 JWT client_assertion
        log.info("[HW Login] 开始生成 JWT client_assertion");
        String clientAssertion;
        try {
            clientAssertion = buildClientAssertion(subAccount, projectId, keyId, privateKey);
            log.info("[HW Login] JWT 生成成功");
        } catch (Exception e) {
            log.error("[HW Login] 生成 JWT 失败", e);
            throw new BusinessException("login.huawei.token.failed");
        }

        // 2. 授权码换用户级 access_token
        log.info("[HW Login] 开始换取用户级 access_token, authCode={}", authCode);
        String tokenBody = JSON.toJSONString(new HashMap<String, Object>() {{
            put("grant_type", "authorization_code");
            put("code", authCode);
            put("client_assertion_type", CLIENT_ASSERTION_TYPE);
            put("client_assertion", clientAssertion);
        }});

        HttpResponse tokenResp = HttpRequest.post(HW_TOKEN_URL)
                .header("x-client-id", projectId)
                .header("x-agent-client-id", subAccount)
                .header("Content-Type", "application/json")
                .body(tokenBody)
                .execute();

        log.info("[HW Login] 换取 token 响应 status={}", tokenResp.getStatus());
        String tokenRespStr = tokenResp.body();
        log.info("[HW Login] 换取 token 响应 body={}", tokenRespStr);

        Map<?, ?> tokenMap = JSON.parseObject(tokenRespStr, Map.class);
        if (tokenMap == null) {
            throw new BusinessException("login.huawei.token.failed");
        }

        String accessToken = (String) tokenMap.get("access_token");
        if (StringUtils.isEmpty(accessToken)) {
            log.error("[HW Login] 换取 access_token 失败, resp={}", tokenRespStr);
            throw new BusinessException("login.huawei.token.failed");
        }

        // 3. 用 access_token 获取用户 openID
        log.info("[HW Login] 开始获取用户信息");
        HttpResponse userInfoResp = HttpRequest.post(HW_USER_INFO_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form("access_token", accessToken)
                .form("getNickName", "1")
                .execute();

        String userInfoStr = userInfoResp.body();
        log.info("[HW Login] 获取用户信息响应: {}", userInfoStr);

        // 检查业务错误（HTTP 200 但 Header 含 NSP_STATUS）
        String nspStatus = userInfoResp.header("NSP_STATUS");
        if (StringUtils.isNotEmpty(nspStatus)) {
            log.error("[HW Login] 业务错误 NSP_STATUS={}, body={}", nspStatus, userInfoStr);
            throw new BusinessException("login.huawei.userinfo.failed");
        }

        Map<?, ?> userInfo = JSON.parseObject(userInfoStr, Map.class);
        if (userInfo == null) {
            throw new BusinessException("login.huawei.userinfo.failed");
        }

        String openId = (String) userInfo.get("openID");
        if (StringUtils.isEmpty(openId)) {
            openId = (String) userInfo.get("openId");
        }

        if (StringUtils.isEmpty(openId)) {
            log.error("[HW Login] 获取 openID 失败, userInfo={}", userInfoStr);
            throw new BusinessException("login.huawei.openid.failed");
        }

        log.info("[HW Login] 登录成功, openId={}", openId);
        return openId;
    }

    /**
     * 从 backend 获取华为应用凭证（projectId / subAccount）。
     * GET {backendUrl}/hw_agcp/auth/app_id?appId={appId}
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchHwAppCredentials(String appId) {
        try {
            String url = backendUrl + "/hw_agcp/auth/app_id?appId=" + appId;
            log.info("[HW Login] 请求 backend 凭证接口, url={}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Key", backendAuthKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String respStr = response.getBody();
            log.info("[HW Login] backend 凭证接口响应: {}", respStr);
            Map<String, Object> resp = JSON.parseObject(respStr, Map.class);
            if (resp != null && resp.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                Map<String, String> result = new HashMap<>();
                result.put("projectId",  String.valueOf(data.getOrDefault("hw_app_id", "")));
                result.put("subAccount", String.valueOf(data.getOrDefault("platform_app_id", "")));
                return result;
            }
            log.warn("[HW Login] backend 凭证接口返回数据为空, appId={}", appId);
        } catch (Exception e) {
            log.error("[HW Login] 获取华为应用凭证失败 appId={}", appId, e);
        }
        return new HashMap<>();
    }

    /**
     * 构建 PS256 签名的 JWT（client_assertion）。
     * Header: {"alg":"PS256","typ":"JWT","kid":"<keyId>"}
     * Payload: {"iss":"<subAccount>","sub":"<projectId>","aud":"...","iat":...,"exp":...}
     */
    private String buildClientAssertion(String subAccount, String projectId, String keyId, String privateKeyPem) throws Exception {
        // 解析 PEM 私钥
        String keyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        // 构建 Header 和 Payload
        String header = base64UrlEncode(
                ("{\"alg\":\"PS256\",\"typ\":\"JWT\",\"kid\":\"" + keyId + "\"}").getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis() / 1000;
        String payload = base64UrlEncode(
                ("{\"iss\":\"" + subAccount + "\",\"sub\":\"" + projectId +
                 "\",\"aud\":\"" + JWT_AUD + "\",\"iat\":" + now + ",\"exp\":" + (now + 3600) + "}")
                .getBytes(StandardCharsets.UTF_8));

        // PS256 签名（SHA256withRSAandMGF1 即 RSASSA-PSS with SHA-256）
        String signingInput = header + "." + payload;
        Signature sig = Signature.getInstance("SHA256withRSAandMGF1");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String signature = base64UrlEncode(sig.sign());

        return signingInput + "." + signature;
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
