package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.system.entity.UserBalance;
import com.kuafuai.system.mapper.UserBalanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 积分/余额相关 DB 操作统一走 DEFAULT 数据源。
 * 所有方法使用 REQUIRES_NEW 强制新开事务，确保从 DEFAULT 拿到新连接。
 * 调用方需在调用前将 DynamicDataSourceContextHolder 切到 DEFAULT。
 */
@Service
@Slf4j
public class DefaultDbHelper {

    @Resource
    private UserBalanceMapper userBalanceMapper;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int forceDeductBalance(Long userId, BigDecimal amount) {
        return userBalanceMapper.forceDeductBalance(userId, amount);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public int increaseBalance(Long userId, BigDecimal amount) {
        return userBalanceMapper.increaseBalance(userId, amount);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public UserBalance getByUserId(Long userId) {
        LambdaQueryWrapper<UserBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBalance::getUserId, userId);
        return userBalanceMapper.selectOne(queryWrapper);
    }
}
