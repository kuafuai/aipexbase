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
public class TextCensorRequest extends AbstractModel {


    @Expose(serialize = false, deserialize = false)
    private String accessToken;

    @Expose
    @SerializedName("text")
    private String text;

    @Override
    protected void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "access_token", this.accessToken);

        this.set("text", this.text);

        this.set("strategyId", 2000391);
        this.setHeaderContentType("application/x-www-form-urlencoded");
    }

}
