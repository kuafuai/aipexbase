package com.kuafuai.login.entity;

import lombok.Data;

/**
 * Google Identity Services (GIS) ID Token 登录请求
 * 前端通过 GIS 拿到 Google 签发的 JWT (ID Token) 后传给后端验签
 */
@Data
public class GoogleIdTokenLoginRequest {

    /**
     * Google 签发的 ID Token (JWT)
     * 由前端 google.accounts.id 回调中的 response.credential 拿到
     */
    private String idToken;

    /**
     * 关联表 (可选)
     * 用户数据存储的表名，不传则使用当前 app 默认 authTable
     */
    private String relevanceTable;
}
