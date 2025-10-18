package com.kuafuai.manage.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuafuai.common.domin.PageRequest;
import lombok.Data;

@Data
public class AppVo extends PageRequest {
    private String name;
    private String appName;
    private Boolean needAuth;
    private String authTable;
    private String configJson;
}
