package com.kuafuai.manage.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理API上下文信息
 * 用于在Filter验证通过后，传递当前操作人信息给业务层
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageApiContext {

    /**
     * Token类型：USER / COMPANY
     */
    private String tokenType;

    /**
     * Token ID
     */
    private Long tokenId;

    /**
     * 场景1：外部用户ID（固化在token中）
     */
    private String userId;

    /**
     * 场景2：企业ID
     */
    private String companyId;

    /**
     * Token名称
     */
    private String tokenName;

    /**
     * 是否是用户级Token
     */
    public boolean isUserToken() {
        return "USER".equals(tokenType);
    }

    /**
     * 是否是企业级Token
     */
    public boolean isCompanyToken() {
        return "COMPANY".equals(tokenType);
    }
}
