package com.kuafuai.manage.entity.vo;


import lombok.Data;

@Data
public class DynamicApiVo {
    private String keyName;
    private String appId;
    private String description;
    private String url;
    private String method;
    private String bodyTemplate;
    private String headerTemplate;
    private String dataRaw;
    private String vars;
}
