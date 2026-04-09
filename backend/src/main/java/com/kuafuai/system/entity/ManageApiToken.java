package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 管理API Token实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("manage_api_token")
public class ManageApiToken {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * API Token
     */
    private String token;

    /**
     * Token类型：USER-用户级, COMPANY-企业级
     */
    private String tokenType;

    /**
     * Token名称/描述
     */
    private String name;

    /**
     * 关联的用户ID（场景1使用）
     */
    private String userId;

    /**
     * 企业标识（场景2使用）
     */
    private String companyId;

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 最后使用时间
     */
    private Date lastUsedTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 备注
     */
    private String remark;

    /**
     * Token类型枚举
     */
    public static class TokenType {
        public static final String USER = "USER";
        public static final String COMPANY = "COMPANY";
    }

    /**
     * Token状态枚举
     */
    public static class Status {
        public static final int DISABLED = 0;
        public static final int ENABLED = 1;
    }
}
