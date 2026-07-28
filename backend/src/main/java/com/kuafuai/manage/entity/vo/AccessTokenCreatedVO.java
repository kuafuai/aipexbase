package com.kuafuai.manage.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Access Token 创建响应. 明文 token 只在这里出现一次, 之后 list 只返回 masked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenCreatedVO {
    private Long id;
    private String name;
    /** Plaintext token, e.g. "mgt_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" — 一次性 */
    private String token;
    private Date expireTime;
}
