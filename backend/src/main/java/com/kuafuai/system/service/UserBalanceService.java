package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.UserBalance;

import java.math.BigDecimal;

public interface UserBalanceService extends IService<UserBalance> {

    /**
     * 获取用户余额
     * @param userId 用户ID
     * @return 用户余额对象
     */
    UserBalance getByUserId(Long userId);

    /**
     * 检查用户余额是否充足
     * @param userId 用户ID
     * @param amount 需要的金额
     * @return 是否充足
     */
    boolean checkBalance(Long userId, BigDecimal amount);

    /**
     * 扣减用户余额(事务方法)
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 是否成功
     */
    boolean deductBalance(Long userId, BigDecimal amount);
    
    /**
     * 增加用户余额(事务方法)
     * @param userId 用户ID
     * @param amount 增加金额
     * @return 是否成功
     */
    boolean increaseBalance(Long userId, BigDecimal amount);
    
    // 新增基于codeflying_user_id的方法
    
    /**
     * 根据codeflying_user_id获取用户余额
     * @param codeFlyingUserId 码上飞用户ID
     * @return 用户余额对象
     */
    UserBalance getByCodeFlyingUserId(String codeFlyingUserId);

    /**
     * 增加用户余额(事务方法)
     * @param codeFlyingUserId 码上飞用户ID
     * @param amount 增加金额
     * @return 是否成功
     */
    boolean increaseBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount);
}