package com.kuafuai.dynamic.event;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TextCensorResponse {


    @Expose
    @SerializedName("error_code")
    private Integer errorCode;

    @Expose
    @SerializedName("error_msg")
    private String errorMsg;

    @Expose
    @SerializedName("conclusionType")
    private Integer conclusionType;

    @Expose
    @SerializedName("conclusion")
    private String conclusion;

}
