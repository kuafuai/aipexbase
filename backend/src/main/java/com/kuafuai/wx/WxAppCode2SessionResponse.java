package com.kuafuai.wx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WxAppCode2SessionResponse extends WxAppBaseResponse {

    @Expose
    @SerializedName("openid")
    private String openId;

    @Expose
    @SerializedName("session_key")
    private String session_key;
}
