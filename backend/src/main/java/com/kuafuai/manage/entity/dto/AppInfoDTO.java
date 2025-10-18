package com.kuafuai.manage.entity.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppInfoDTO {

    private String appId;
    private String appName;

    private String iconUrl;

    private Boolean needAuth;
    private String authTable;

    private Boolean enablePermission;
    private Boolean enableWebConsole;

    private String status;
    private String description;
    private String configJson;

    private Long owner;
    private int appTableNum;
}
