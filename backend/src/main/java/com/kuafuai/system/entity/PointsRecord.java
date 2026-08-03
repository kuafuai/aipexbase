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
@TableName("points_record")
public class PointsRecord {

    // ── type: 增加/减少 ──
    public static final String TYPE_DEDUCT = "DEDUCT";
    public static final String TYPE_RECHARGE = "RECHARGE";

    // ── sub_type: 扣减来源 ──
    public static final String SUB_DAILY = "DAILY";             // 每日赠送积分扣减
    public static final String SUB_INVITATION = "INVITATION";   // 邀请奖励积分扣减
    public static final String SUB_MONTHLY = "MONTHLY";         // 会员月积分扣减
    public static final String SUB_BALANCE = "BALANCE";         // 加油包余额扣减

    // ── sub_type: 充值来源 ──
    public static final String SUB_PURCHASE = "PURCHASE";       // 积分包购买
    public static final String SUB_REDEMPTION = "REDEMPTION";   // 兑换码兑换
    public static final String SUB_FEEDBACK = "FEEDBACK";       // 客服反馈首次奖励
    public static final String SUB_WORKORDER = "WORKORDER";     // 工单评价首次奖励
    public static final String SUB_REGISTER = "REGISTER";       // 新用户注册赠送
    public static final String SUB_ADMIN = "ADMIN";             // 管理员手动充值
    public static final String SUB_OTHER = "OTHER";             // 其他

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("codeflying_user_id")
    private String codeFlyingUserId;

    /** DEDUCT / RECHARGE */
    private String type;

    /** 具体来源：扣减(DAILY/INVITATION/MONTHLY/BALANCE) 充值(PURCHASE/REDEMPTION/FEEDBACK/...) */
    private String subType;

    /** 金额（正数） */
    private BigDecimal amount;

    private Date createdAt;
}
