package com.kuafuai.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class FrontendErrorMessage {

    @JsonProperty("error_message")
    private String errorMessage;

    private String path;

    private String type;
    @JsonProperty("sub_source")
    private String subSource;
    @JsonProperty("app_id")
    private String  appId;
    @JsonProperty("requirement_id")
    private Integer requirement_id;

}
