package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.UserBalance;
import com.kuafuai.system.entity.Users;
import com.kuafuai.system.mapper.UserBalanceMapper;
import com.kuafuai.system.service.UserBalanceService;
import com.kuafuai.system.service.UsersService;
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
    
    @Resource
    private UsersService usersService;

    @Override
    public UserBalance getByUserId(Long userId) {
        log.info("开始查询用户余额，用户ID: {}", userId);
        LambdaQueryWrapper<UserBalance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBalance::getUserId, userId);
        UserBalance userBalance = getOne(queryWrapper);
        log.info("用户余额查询完成，用户ID: {}，查询结果: {}", userId, userBalance);
        return userBalance;
    }

    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        log.info("开始检查用户余额是否充足，用户ID: {}，需检查金额: {}", userId, amount);
        UserBalance userBalance = getByUserId(userId);
        if (userBalance == null) {
            log.warn("用户余额记录不存在, userId: {}", userId);
            return false;
        }

        if (userBalance.getStatus() != 1) {
            log.warn("用户账户状态异常, userId: {}, status: {}", userId, userBalance.getStatus());
            return false;
        }
        
        boolean isSufficient = userBalance.getBalance().compareTo(amount) >= 0;
        log.info("用户余额检查完成，用户ID: {}，账户余额: {}，需检查金额: {}，余额是否充足: {}", 
                 userId, userBalance.getBalance(), amount, isSufficient);
        return isSufficient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("开始扣减用户余额，用户ID: {}，扣减金额: {}", userId, amount);
        int rows = userBalanceMapper.deductBalance(userId, amount);
        if (rows <= 0) {
            log.error("扣减余额失败, userId: {}, amount: {}", userId, amount);
            return false;
        }
        log.info("扣减余额成功, userId: {}, amount: {}", userId, amount);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseBalance(Long userId, BigDecimal amount) {
        log.info("开始增加用户余额，用户ID: {}，增加金额: {}", userId, amount);
        int rows = userBalanceMapper.increaseBalance(userId, amount);
        if (rows <= 0) {
            log.error("增加余额失败, userId: {}, amount: {}", userId, amount);
            return false;
        }
        log.info("增加余额成功, userId: {}, amount: {}", userId, amount);
        return true;
    }
    
    // 新增基于codeflying_user_id的方法实现
    
    @Override
    public UserBalance getByCodeFlyingUserId(String codeFlyingUserId) {
        log.info("开始查询用户信息，codeFlyingUserId: {}", codeFlyingUserId);
        Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
        if (user == null) {
            log.warn("用户不存在, codeFlyingUserId: {}", codeFlyingUserId);
            return null;
        }
        log.info("用户信息查询完成，codeFlyingUserId: {}，用户ID: {}", codeFlyingUserId, user.getId());
        return getByUserId(user.getId());
    }

    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount) {
        log.info("开始增加用户余额，codeFlyingUserId: {}，增加金额: {}", codeFlyingUserId, amount);
        Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
        if (user == null) {
            log.warn("用户不存在, codeFlyingUserId: {}", codeFlyingUserId);
            return false;
        }
        log.info("用户信息查询完成，codeFlyingUserId: {}，用户ID: {}", codeFlyingUserId, user.getId());
        return increaseBalance(user.getId(), amount);
    }
}