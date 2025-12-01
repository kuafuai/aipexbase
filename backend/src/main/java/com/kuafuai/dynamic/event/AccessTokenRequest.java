package com.kuafuai.dynamic.event;

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
public class AccessTokenRequest extends AbstractModel {

    @Expose
    @SerializedName("grant_type")
    private String grantType = "client_credentials";

    @Expose
    @SerializedName("client_id")
    private String clientId;

    @Expose
    @SerializedName("client_secret")
    private String clientSecret;

    @Override
    protected void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "client_id", this.clientId);
        this.setParamSimple(map, prefix + "client_secret", this.clientSecret);
        this.setParamSimple(map, prefix + "grant_type", "client_credentials");
    }
}
