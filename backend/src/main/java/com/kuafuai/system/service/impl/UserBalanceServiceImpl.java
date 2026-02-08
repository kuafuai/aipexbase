package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.UserBalance;
import com.kuafuai.system.entity.Users;
import com.kuafuai.system.mapper.UserBalanceMapper;
import com.kuafuai.system.service.UserBalanceService;
import com.kuafuai.system.service.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
@Slf4j
public class UserBalanceServiceImpl extends ServiceImpl<UserBalanceMapper, UserBalance> implements UserBalanceService {

    private static final String USER_CODEFLYING_MAPPING_KEY = "user:codeflying:balance:";
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("2.00"); // 默认余额2元

    @Resource
    private UsersService usersService;

    @Autowired(required = false)
    @Qualifier(value = "dataRouterRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

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
        log.info("开始检查用户余额是否充足（仅Redis），用户ID: {}，需检查金额: {}", userId, amount);

        Users user = usersService.getById(userId);
        String codeflyingUserId = user.getCodeFlyingUserId();

        String balanceKey = USER_CODEFLYING_MAPPING_KEY + codeflyingUserId;

        try {
            // 1. 直接从Redis获取余额
            Object balanceObj = redisTemplate.opsForValue().get(balanceKey);

            if (balanceObj == null) {
                // redis 不存在
                UserBalance userBalance = getByUserId(userId);
                balanceObj = userBalance.getBalance();
            }

            // 2. Redis中存在，进行余额检查
            BigDecimal currentBalance = new BigDecimal(balanceObj.toString());
            boolean isSufficient = currentBalance.compareTo(amount) >= 0;

            log.info("用户余额检查完成（Redis余额），用户ID: {}，账户余额: {}，需检查金额: {}，余额是否充足: {}",
                    userId, currentBalance, amount, isSufficient);
            return isSufficient;

        } catch (Exception e) {
            log.error("检查用户余额异常, userId: {}, amount: {}", userId, amount, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("开始扣减用户余额，用户ID: {}，扣减金额: {}", userId, amount);
        Users user = usersService.getById(userId);
        String codeflyingUserId = user.getCodeFlyingUserId();

        String balanceKey = USER_CODEFLYING_MAPPING_KEY + codeflyingUserId;
        try {
            // 2. 安全地扣减余额，避免Redis increment类型问题
            Object currentBalanceObj = redisTemplate.opsForValue().get(balanceKey);
            if (currentBalanceObj == null) {
                // redis 不存在
                UserBalance userBalance = getByUserId(userId);
                currentBalanceObj = userBalance.getBalance();
            }

            String currentBalanceStr = currentBalanceObj.toString();
            BigDecimal currentBalance = new BigDecimal(currentBalanceStr);

            // 计算新的余额
            BigDecimal newBalance = currentBalance.subtract(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.error("余额不足, userId: {}, 当前余额: {}, 扣减金额: {}", userId, currentBalance, amount);
                return false;
            }

            // 使用set操作更新余额（避免increment的类型问题）
            redisTemplate.opsForValue().set(balanceKey, newBalance.toPlainString());
            Double deductedAmount = newBalance.doubleValue();

            log.info("Redis余额扣减成功, userId: {}, 扣减金额: {}, 新余额: {}", userId, amount, deductedAmount);

            // 5. 同步到数据库
            syncBalanceToDb(userId, BigDecimal.valueOf(deductedAmount));

            return true;

        } catch (Exception e) {
            log.error("扣减余额异常, userId: {}, amount: {}", userId, amount, e);
            return false;
        }
    }

    /**
     * 同步余额到数据库
     */
    public void syncBalanceToDb(Long userId, BigDecimal newBalance) {
        try {
            UserBalance userBalance = getByUserId(userId);
            userBalance.setBalance(newBalance);
            updateById(userBalance);
        } catch (Exception e) {
            log.error("同步余额到数据库异常, codeFlyingUserId: {}, newBalance: {}", userId, newBalance, e);
        }
    }

    @Override
    public UserBalance getByCodeFlyingUserId(String codeFlyingUserId) {
        log.info("开始查询用户余额（仅Redis），codeFlyingUserId: {}", codeFlyingUserId);

        String balanceKey = USER_CODEFLYING_MAPPING_KEY + codeFlyingUserId;

        try {
            // 1. 直接从Redis获取余额
            Object balanceObj = redisTemplate.opsForValue().get(balanceKey);

            if (balanceObj != null) {
                // Redis中存在，直接返回
                BigDecimal balance = new BigDecimal(balanceObj.toString());
                log.info("从Redis获取用户余额成功, codeFlyingUserId: {}, balance: {}", codeFlyingUserId, balance);

                return UserBalance.builder()
                        .balance(balance)
                        .frozenBalance(BigDecimal.ZERO)
                        .status(1)
                        .build();
            }

            // 2. Redis中不存在，创建默认余额
            log.info("Redis中不存在用户余额，创建默认余额, codeFlyingUserId: {}: {}", codeFlyingUserId, codeFlyingUserId);
            redisTemplate.opsForValue().set(balanceKey, DEFAULT_BALANCE.toPlainString());

            log.info("创建默认用户余额成功, codeFlyingUserId: {}, balance: {}: {}", codeFlyingUserId, codeFlyingUserId, DEFAULT_BALANCE);

            return UserBalance.builder()
                    .balance(DEFAULT_BALANCE)
                    .frozenBalance(BigDecimal.ZERO)
                    .status(1)
                    .build();

        } catch (Exception e) {
            log.error("查询用户余额异常, codeFlyingUserId: {}:{}", codeFlyingUserId, codeFlyingUserId, e);
            throw new RuntimeException("查询用户余额失败", e);
        }
    }


    @Override
    public boolean increaseBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount) {
        log.info("开始增加用户余额（仅Redis），codeFlyingUserId: {}，增加金额: {}", codeFlyingUserId, amount);

        String balanceKey = USER_CODEFLYING_MAPPING_KEY + codeFlyingUserId;

        try {
            // 1. 从Redis获取当前余额
            Object balanceObj = redisTemplate.opsForValue().get(balanceKey);
            BigDecimal currentBalance;

            if (balanceObj != null) {
                currentBalance = new BigDecimal(balanceObj.toString());
            } else {
                // Redis中不存在，使用默认余额
                log.info("Redis中不存在用户余额，使用默认余额, codeFlyingUserId: {}:{}", codeFlyingUserId, codeFlyingUserId);
                currentBalance = DEFAULT_BALANCE;
            }

            // 2. 计算新余额
            BigDecimal newBalance = currentBalance.add(amount);

            // 3. 更新Redis中的余额
            redisTemplate.opsForValue().set(balanceKey, newBalance.toPlainString());

            log.info("Redis余额增加成功, codeFlyingUserId: {}:{}, 原余额: {}, 增加金额: {}, 新余额: {}", codeFlyingUserId, codeFlyingUserId, currentBalance, amount, newBalance);

            return true;

        } catch (Exception e) {
            log.error("增加用户余额异常, codeFlyingUserId: {}:{}, amount: {}", codeFlyingUserId, codeFlyingUserId, amount, e);
            return false;
        }
    }
}