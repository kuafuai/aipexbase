package com.kuafuai.manage.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Masked Access Token view for list responses.
 *
 * The raw token (mgt_XXXX...XXXX) is never returned by the list endpoint.
 * Only the prefix (mgt_ + first 4) and suffix (last 4) are kept; middle bullets.
 * Full plaintext is only returned once at creation time via AccessTokenCreatedVO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenMaskedVO {
    private Long id;
    private String name;
    private String remark;
    /** e.g. "mgt_1a2b••••••••••••••••••••••••2d3f" */
    private String tokenMasked;
    /** 0=DISABLED, 1=ENABLED */
    private Integer status;
    private Date expireTime;
    private Date lastUsedTime;
    private Date createTime;

    public static String mask(String rawToken) {
        if (rawToken == null || rawToken.length() <= 16) {
            return rawToken;
        }
        String prefix = rawToken.substring(0, 8);
        String suffix = rawToken.substring(rawToken.length() - 4);
        return prefix + "••••••••••••••••••••••••" + suffix;
    }
}
