package com.kuafuai.wx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WxAppPhoneNumberResponse extends WxAppBaseResponse {

    @Expose
    @SerializedName("phone_info")
    private WxAppPhoneNumber phoneInfo;
}
