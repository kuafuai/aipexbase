package com.kuafuai.pay.stats;

import com.kuafuai.pay.enums.PayStatus;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 支付状态的来回转化
 */
public class PayStatusStateMachine {

    // 定义允许的状态转换
    private static final Map<PayStatus, Set<PayStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        // 初始化状态转换规则
        ALLOWED_TRANSITIONS.put(PayStatus.UNPAID, EnumSet.of(
                PayStatus.PAYING,
                PayStatus.CANCELLED,
                PayStatus.TIMEOUT
        ));

        ALLOWED_TRANSITIONS.put(PayStatus.PAYING, EnumSet.of(
                PayStatus.PAID_SUCCESS,
                PayStatus.PAID_FAIL,
                PayStatus.CANCELLED,
                PayStatus.TIMEOUT
        ));

        ALLOWED_TRANSITIONS.put(PayStatus.PAID_SUCCESS, EnumSet.of(
                PayStatus.REFUNDED,
                PayStatus.PARTIALLY_REFUNDED
        ));

        ALLOWED_TRANSITIONS.put(PayStatus.PAID_FAIL, EnumSet.of(
                PayStatus.UNPAID
        ));
        ALLOWED_TRANSITIONS.put(PayStatus.PENDING_RECEIPT, EnumSet.of(
                PayStatus.CONFIRM
        ));

        ALLOWED_TRANSITIONS.put(PayStatus.CANCELLED, EnumSet.noneOf(PayStatus.class));
        ALLOWED_TRANSITIONS.put(PayStatus.REFUNDED, EnumSet.noneOf(PayStatus.class));
        ALLOWED_TRANSITIONS.put(PayStatus.PARTIALLY_REFUNDED, EnumSet.noneOf(PayStatus.class));
        ALLOWED_TRANSITIONS.put(PayStatus.TIMEOUT, EnumSet.noneOf(PayStatus.class));
    }

    /**
     * 检查状态转换是否合法
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否允许转换
     */
    public static boolean canTransition(PayStatus currentStatus, PayStatus newStatus) {
        Set<PayStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(newStatus);
    }

    /**
     * 获取所有允许的下一状态
     * @param currentStatus 当前状态
     * @return 允许的状态集合
     */
    public static Set<PayStatus> getAllowedTransitions(PayStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(PayStatus.class));
    }

    /**
     * 是否是终态（不可再改变的状态）
     */
    public static boolean isFinalStatus(PayStatus status) {
        return getAllowedTransitions(status).isEmpty();
    }
}