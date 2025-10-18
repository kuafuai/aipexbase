package com.kuafuai.wx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WxAppPhoneNumber {
    @Expose
    @SerializedName("phoneNumber")
    private String phoneNumber;

    @Expose
    @SerializedName("purePhoneNumber")
    private String purePhoneNumber;

    @Expose
    @SerializedName("countryCode")
    private String countryCode;
}
