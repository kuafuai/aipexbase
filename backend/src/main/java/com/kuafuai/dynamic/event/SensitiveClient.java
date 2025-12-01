package com.kuafuai.dynamic.event;

import com.kuafuai.common.http.AbstractClient;
import com.kuafuai.common.util.JSON;

public class SensitiveClient extends AbstractClient {

    public AccessTokenResponse getAccessToken(AccessTokenRequest request) {
        String value = this.internalRequest("https://aip.baidubce.com/oauth/2.0/token", "GET", request);
        return JSON.parseObject(value, AccessTokenResponse.class);
    }

    public TextCensorResponse textCensor(TextCensorRequest request) {
        String value = this.internalRequest("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined", "POST", request);
        return JSON.parseObject(value, TextCensorResponse.class);
    }


}
