package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.UserBalance;
import com.kuafuai.system.mapper.UserBalanceMapper;
import com.kuafuai.system.service.UserBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
@Slf4j
public class UserBalanceServiceImpl extends ServiceImpl<UserBalanceMapper, UserBalance> implements UserBalanceService {

    @Resource
    private UserBalanceMapper userBalanceMapper;

    @Override
    public UserBalance getByUserId(Long userId) {
        LambdaQueryWrapper<UserBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBalance::getUserId, userId);
        return getOne(queryWrapper);
    }

    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        UserBalance userBalance = getByUserId(userId);
        if (userBalance == null) {
            log.warn("用户余额记录不存在, userId: {}", userId);
            return false;
        }

        if (userBalance.getStatus() != 1) {
            log.warn("用户账户状态异常, userId: {}, status: {}", userId, userBalance.getStatus());
            return false;
        }

        return userBalance.getBalance().compareTo(amount) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        int rows = userBalanceMapper.deductBalance(userId, amount);
        if (rows <= 0) {
            log.error("扣减余额失败, userId: {}, amount: {}", userId, amount);
            return false;
        }
        log.info("扣减余额成功, userId: {}, amount: {}", userId, amount);
        return true;
    }
}
