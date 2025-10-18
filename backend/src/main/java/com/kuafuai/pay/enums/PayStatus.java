package com.kuafuai.pay.enums;

/**
 * 支付状态枚举
 * 使用字符串类型的code
 */
public enum PayStatus {
    /**
     * 待支付（已创建订单但未支付）
     */
    UNPAID("unpaid", "待支付"),

    /**
     * 支付中（已发起支付但未确认结果）
     */
    PAYING("paying", "支付中"),

    /**
     * 支付成功
     */
    PAID_SUCCESS("paid_success", "支付成功"),

    /**
     * 支付失败
     */
    PAID_FAIL("paid_failed", "支付失败"),

    /**
     * 已取消（用户或系统取消）
     */
    CANCELLED("cancelled", "已取消"),

    /**
     * 已退款
     */
    REFUNDED("refunded", "已退款"),

    /**
     * 部分退款
     */
    PARTIALLY_REFUNDED("partially_refunded", "部分退款"),

    /**
     * 支付超时
     */
    TIMEOUT("timeout", "支付超时"),

    CLOSED("closed", "关闭订单"),


    PENDING_RECEIPT("pending_receipt", "待收货"),
    /**
     * 确认收货
     */

    CONFIRM("confirm", "确认收货");






    private final String code;
    private final String description;

    PayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static PayStatus getByCode(String code) {
        for (PayStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}