package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("token_billing_record")
public class TokenBillingRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String codeFlyingUserId;

    private String chatId;

    private Long messageId;

    private String agentId;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer cachedTokens;

    private Integer totalTokens;

    private BigDecimal promptAmount;

    private BigDecimal completionAmount;

    private BigDecimal cacheAmount;

    private BigDecimal totalAmount;

    private Date messageTime;

    private Date createdAt;
}
