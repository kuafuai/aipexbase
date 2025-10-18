package com.kuafuai.wx;

import com.kuafuai.common.http.AbstractClient;
import com.kuafuai.common.util.JSON;

public class WxAppClient extends AbstractClient {


    public WxAppClient() {
        super();
    }

    public WxAppCode2SessionResponse code2Session(WxAppCode2SessionRequest request) {
        String value = this.internalRequest("https://api.weixin.qq.com/sns/jscode2session", "GET", request);
        WxAppCode2SessionResponse response = JSON.parseObject(value, WxAppCode2SessionResponse.class);
        return response;
    }

    public WxAppAccessTokenResponse getAccessToken(WxAppAccessTokenRequest request) {
        String value = this.internalRequest("https://api.weixin.qq.com/cgi-bin/token", "GET", request);
        WxAppAccessTokenResponse response = JSON.parseObject(value, WxAppAccessTokenResponse.class);
        return response;
    }

    public WxAppPhoneNumberResponse getPhoneNumber(WxAppPhoneNumberRequest request) {
        String value = this.internalRequest("https://api.weixin.qq.com/wxa/business/getuserphonenumber", "POST", request);
        log.info("==================" + value);
        WxAppPhoneNumberResponse response = JSON.parseObject(value, WxAppPhoneNumberResponse.class);
        return response;
    }

    /**
     * h5 获取openId
     *
     * @param request re
     * @return rsp
     */
    public WxWebCode2TokenResponse code2Session(WxWebCode2TokenRequest request) {
        String value = this.internalRequest("https://api.weixin.qq.com/sns/oauth2/access_token", "GET", request);

        WxWebCode2TokenResponse response = JSON.parseObject(value, WxWebCode2TokenResponse.class);
        return response;
    }

}
