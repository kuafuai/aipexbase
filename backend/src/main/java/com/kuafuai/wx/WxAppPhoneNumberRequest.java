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
public class WxAppPhoneNumberRequest extends AbstractModel {

    @Expose(serialize = false, deserialize = false)
    private String accessToken;

    @Expose
    @SerializedName("code")
    private String code;

    @Override
    protected void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "access_token", this.accessToken);
    }
}
