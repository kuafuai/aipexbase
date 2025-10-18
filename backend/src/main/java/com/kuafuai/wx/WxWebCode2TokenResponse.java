package com.kuafuai.wx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WxWebCode2TokenResponse extends WxAppBaseResponse {
    @Expose
    @SerializedName("openid")
    private String openId;

    @Expose
    @SerializedName("access_token")
    private String accessToken;

    @Expose
    @SerializedName("scope")
    private String scope;

    @Expose
    @SerializedName("unionid")
    private String unionId;
}
