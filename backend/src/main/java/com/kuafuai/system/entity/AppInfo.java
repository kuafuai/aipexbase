package com.kuafuai.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("app_info")
public class AppInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
}
