package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("balance_package")
public class BalancePackage {

    /** 有效 */
    public static final int STATUS_ACTIVE = 1;
    /** 已过期或已用完 */
    public static final int STATUS_EXPIRED = 0;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("codeflying_user_id")
    private String codeFlyingUserId;

    /** 充值总额 */
    private BigDecimal totalAmount;

    /** 剩余可用 */
    private BigDecimal remainingAmount;

    /** 来源：PURCHASE/REDEMPTION/FEEDBACK/WORKORDER/REGISTER/ADMIN/OTHER */
    private String source;

    /** 过期时间 */
    private Date expiredAt;

    /** 1-有效 0-已过期/已用完 */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
