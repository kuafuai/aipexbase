package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.BalancePackage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BalancePackageService extends IService<BalancePackage> {

    /**
     * 新增加油包（充值时调用）
     *
     * @param codeFlyingUserId 用户ID
     * @param amount           充值金额
     * @param source           来源类型
     * @return 新建的加油包记录
     */
    BalancePackage addPackage(String codeFlyingUserId, BigDecimal amount, String source);

    /**
     * 按过期时间优先扣减加油包（FIFO）
     * 自动过期清理 + 逐包扣减
     *
     * @param codeFlyingUserId 用户ID
     * @param amount           需要扣减的总额
     * @return 实际扣减的金额（可能小于 amount 如果余额不足）
     */
    BigDecimal deductFromPackages(String codeFlyingUserId, BigDecimal amount);

    /**
     * 获取有效加油包的汇总信息
     *
     * @param codeFlyingUserId 用户ID
     * @return { "total": 总额, "remaining": 剩余, "packages": [明细列表] }
     */
    Map<String, Object> getBalanceSummary(String codeFlyingUserId);

    /**
     * 获取有效余额总和（清理过期后）
     */
    BigDecimal getValidBalance(String codeFlyingUserId);

    /**
     * 清理过期的加油包
     */
    void cleanExpiredPackages(String codeFlyingUserId);
}
