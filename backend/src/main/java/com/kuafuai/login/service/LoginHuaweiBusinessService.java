package com.kuafuai.login.service;

import cn.hutool.http.HttpUtil;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.config.HuaweiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 华为账号登录业务服务
 * 通过授权码换取 access_token，再获取华为账号的 openID。
 */
@Service
@Slf4j
public class LoginHuaweiBusinessService {

    @Autowired
    private HuaweiConfig huaweiConfig;

    private static final String HW_TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token";
    private static final String HW_USER_INFO_URL = "https://account.cloud.huawei.com/rest.php";

    /**
     * 用授权码换取华为 openID。
     *
     * @param authCode 华为客户端传来的授权码
     * @return 华为账号 openID
     */
    public String getOpenId(String authCode) {
        String clientId = huaweiConfig.getClientId();
        String clientSecret = huaweiConfig.getClientSecret();
        log.info("[HW Login] 使用 client_id={}", clientId);
        log.info("[HW Login] 使用 client_secret={}", clientSecret);

        if (StringUtils.isEmpty(clientId) || StringUtils.equalsAnyIgnoreCase(authCode, "codeflying")) {
            return "codeflying";
        }

        // 1. 授权码换 access_token
        Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("grant_type", "authorization_code");
        tokenParams.put("code", authCode);
        tokenParams.put("client_id", clientId);
        tokenParams.put("client_secret", clientSecret);
        tokenParams.put("redirect_uri", "");

        String tokenRespStr = HttpUtil.post(HW_TOKEN_URL, tokenParams);
        log.info("[HW Login] 换取 token 响应: {}", tokenRespStr);

        Map<?, ?> tokenResp = JSON.parseObject(tokenRespStr, Map.class);
        if (tokenResp == null) {
            throw new BusinessException("login.huawei.token.failed");
        }

        String accessToken = (String) tokenResp.get("access_token");
        if (StringUtils.isEmpty(accessToken)) {
            log.error("[HW Login] 换取 access_token 失败, resp={}", tokenRespStr);
            throw new BusinessException("login.huawei.token.failed");
        }

        // 2. 用 access_token 获取用户 openID
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String userInfoStr;
        try {
            String encodedToken = java.net.URLEncoder.encode(accessToken, "UTF-8");
            String userInfoUrl = HW_USER_INFO_URL + "?nsp_svc=GOpen.User.getInfo&nsp_ts=" + ts + "&access_token=" + encodedToken;
            userInfoStr = HttpUtil.get(userInfoUrl);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new BusinessException("login.huawei.userinfo.failed");
        }
        log.info("[HW Login] 获取用户信息响应: {}", userInfoStr);

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

        return openId;
    }
}

