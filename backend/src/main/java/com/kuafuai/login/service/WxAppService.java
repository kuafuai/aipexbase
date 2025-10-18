package com.kuafuai.login.service;

import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.config.WechatConfig;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.wx.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class WxAppService {

    private WxAppClient wxAppClient = new WxAppClient();

    @Resource
    private Cache cache;

    private static final String KEY = "wx:accesstoken";


    public String getOpenId(String code) {

        WechatConfig wechatConfig = WechatConfig.builder()
                .appId(GlobalAppIdFilter.getAppId())
                .build();

        WxAppCode2SessionResponse response = wxAppClient.code2Session(WxAppCode2SessionRequest.builder()
                .appId(wechatConfig.getAppId())
                .appSecret(wechatConfig.getAppSecret())
                .code(code)
                .grantType("authorization_code")
                .build());
        if (response.getErrcode() != null && response.getErrcode() > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR.getCode(), "小程序验证失败");
        }
        return response.getOpenId();
    }

    public String getAccessToken() {

        WechatConfig wechatConfig = WechatConfig.builder()
                .appId(GlobalAppIdFilter.getAppId())
                .build();

        String accessToken = cache.getCacheObject(KEY);

        if (StringUtils.isNotEmpty(accessToken)) {
            return accessToken;
        }

        WxAppAccessTokenRequest request = WxAppAccessTokenRequest.builder()
                .appId(wechatConfig.getAppId())
                .appSecret(wechatConfig.getAppId())
                .grantType("client_credential")
                .build();

        WxAppAccessTokenResponse response = wxAppClient.getAccessToken(request);
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "token获取失败");
        }

        accessToken = response.getAccessToken();
        int expires_in = response.getExpiresIn();
        // 写入缓存中
        cache.setCacheObject(KEY, accessToken, expires_in, TimeUnit.SECONDS);

        return accessToken;
    }


    /**
     * 获取手机号
     *
     * @param code 授权码
     * @return 手机号
     */
    public String getPhone(String code) {
        String accessToken = getAccessToken();

        WxAppPhoneNumberRequest request = WxAppPhoneNumberRequest.builder()
                .accessToken(accessToken)
                .code(code)
                .build();

        WxAppPhoneNumberResponse response = wxAppClient.getPhoneNumber(request);

        if (response.getErrcode() != null && response.getErrcode() != 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机号获取失败");
        }

        return response.getPhoneInfo().getPhoneNumber();

    }
}
