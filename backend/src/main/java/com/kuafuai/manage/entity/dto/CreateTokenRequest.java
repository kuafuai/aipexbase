package com.kuafuai.manage.entity.dto;

import lombok.Data;

import java.util.Date;

/**
 * 创建Token请求
 */
@Data
public class CreateTokenRequest {
    /**
     * Token名称
     */
    private String name;

    /**
     * 用户ID（场景1必填）
     */
    private String userId;

    /**
     * 企业ID（场景2必填）
     */
    private String companyId;

    /**
     * 企业名称（场景2可选）
     */
    private String companyName;

    /**
     * 过期时间（可选）
     */
    private Date expireTime;

    /**
     * 备注
     */
    private String remark;
}
