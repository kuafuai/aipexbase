package com.kuafuai.wx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kuafuai.common.http.AbstractModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WxAppCode2SessionRequest extends AbstractModel {

    @Expose
    @SerializedName("appid")
    private String appId;

    @Expose
    @SerializedName("secret")
    private String appSecret;

    @Expose
    @SerializedName("js_code")
    private String code;

    @Expose
    @SerializedName("grant_type")
    private String grantType = "authorization_code";

    @Override
    protected void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "appid", this.appId);
        this.setParamSimple(map, prefix + "secret", this.appSecret);
        this.setParamSimple(map, prefix + "js_code", this.code);
        this.setParamSimple(map, prefix + "grant_type", this.grantType);
    }
}
