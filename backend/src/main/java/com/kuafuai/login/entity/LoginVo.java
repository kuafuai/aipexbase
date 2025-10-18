package com.kuafuai.login.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginVo {
    @Schema(description = "小程序code")
    private String code;

    @Schema(description = "用户名/手机号")
    private String phone;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "state")
    private String state;

    @Schema(description = "关联表")
    private String relevanceTable;

    private String openId;

}
