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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class UserBalanceServiceImpl extends ServiceImpl<UserBalanceMapper, UserBalance> implements UserBalanceService {

    private static final String USER_CODEFLYING_MAPPING_KEY = "user:codeflying:balance:";
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("2.00");

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${backend.url:}")
    private String backendUrl;

    @Value("${backend.internal.auth-key:b54igLGJ1DpB8OMF}")
    private String backendAuthKey;

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
        log.info("开始检查用户余额是否充足，用户ID: {}，需检查金额: {}", userId, amount);

        if (!isRedisEnabled()) {
            UserBalance ub = getByUserId(userId);
            if (ub == null || ub.getStatus() != 1) {
                log.warn("用户余额记录不存在或账户异常, userId: {}", userId);
                return false;
            }
            boolean sufficient = ub.getBalance().compareTo(amount) >= 0;
            log.info("余额检查完成（DB），userId: {}, 余额: {}, 所需: {}, 充足: {}", userId, ub.getBalance(), amount, sufficient);
            return sufficient;
        }

        try {
            Users user = usersService.getById(userId);
            BigDecimal balance = fetchCurrentBalance(user.getCodeFlyingUserId());
            boolean sufficient = balance.compareTo(amount) >= 0;
            log.info("余额检查完成（Redis），userId: {}, 余额: {}, 所需: {}, 充足: {}", userId, balance, amount, sufficient);
            return sufficient;
        } catch (Exception e) {
            log.error("检查用户余额异常, userId: {}, amount: {}", userId, amount, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("开始扣减用户余额，用户ID: {}，扣减金额: {}", userId, amount);
        if (!isRedisEnabled()) {
            int rows = getBaseMapper().deductBalance(userId, amount);
            if (rows <= 0) {
                log.error("余额不足或账户异常（DB），userId: {}, amount: {}", userId, amount);
                return false;
            }
            log.info("DB余额扣减成功, userId: {}, 扣减金额: {}", userId, amount);
            return true;
        }

        try {
            Users user = usersService.getById(userId);
            String codeflyingUserId = user.getCodeFlyingUserId();

            BigDecimal currentBalance = fetchCurrentBalance(codeflyingUserId);
            BigDecimal newBalance = currentBalance.subtract(amount);

            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.error("余额不足（Redis）, userId: {}, 当前余额: {}, 扣减金额: {}", userId, currentBalance, amount);
                return false;
            }

            redisTemplate.opsForValue().set(USER_CODEFLYING_MAPPING_KEY + codeflyingUserId, newBalance.toPlainString());
            log.info("Redis余额扣减成功, userId: {}, 扣减金额: {}, 新余额: {}", userId, amount, newBalance);

            notifyBackendStat(Long.parseLong(codeflyingUserId), "consume", amount);
            return true;

        } catch (Exception e) {
            log.error("扣减余额异常, userId: {}, amount: {}", userId, amount, e);
            return false;
        }
    }

    public void syncBalanceToDb(Long userId, BigDecimal newBalance) {
        try {
            UserBalance userBalance = getByUserId(userId);
            userBalance.setBalance(newBalance);
            updateById(userBalance);
        } catch (Exception e) {
            log.error("同步余额到数据库异常, userId: {}, newBalance: {}", userId, newBalance, e);
        }
    }

    @Override
    public UserBalance getByCodeFlyingUserId(String codeFlyingUserId) {
        log.info("开始查询用户余额，codeFlyingUserId: {}", codeFlyingUserId);

        if (!isRedisEnabled()) {
            Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
            if (user == null) {
                log.warn("用户不存在, codeFlyingUserId: {}", codeFlyingUserId);
                return null;
            }
            return getByUserId(user.getId());
        }

        try {
            BigDecimal balance = fetchCurrentBalance(codeFlyingUserId);
            return UserBalance.builder()
                    .balance(balance)
                    .frozenBalance(BigDecimal.ZERO)
                    .status(1)
                    .build();
        } catch (Exception e) {
            log.error("查询用户余额异常, codeFlyingUserId: {}", codeFlyingUserId, e);
            throw new RuntimeException("查询用户余额失败", e);
        }
    }

    @Override
    public boolean increaseBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount) {

        if (!isRedisEnabled()) {
            Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
            int rows = getBaseMapper().increaseBalance(user.getId(), amount);
            if (rows <= 0) {
                log.error("增加余额失败（DB）, codeFlyingUserId: {}", codeFlyingUserId);
                return false;
            }
            log.info("DB余额增加成功, codeFlyingUserId: {}, 增加金额: {}", codeFlyingUserId, amount);
            return true;
        }

        try {
            BigDecimal currentBalance = fetchCurrentBalance(codeFlyingUserId);
            BigDecimal newBalance = currentBalance.add(amount);

            redisTemplate.opsForValue().set(USER_CODEFLYING_MAPPING_KEY + codeFlyingUserId, newBalance.toPlainString());

            log.info("Redis余额增加成功, codeFlyingUserId: {}, 原余额: {}, 增加金额: {}, 新余额: {}", codeFlyingUserId, currentBalance, amount, newBalance);

            notifyBackendStat(Long.parseLong(codeFlyingUserId), "recharge", amount);
            return true;

        } catch (Exception e) {
            log.error("增加用户余额异常, codeFlyingUserId: {}, amount: {}", codeFlyingUserId, amount, e);
            return false;
        }
    }

    /**
     * 从 Redis 取余额，miss 时初始化为默认余额。
     * 调用方须保证 isRedisEnabled() == true。
     */
    private BigDecimal fetchCurrentBalance(String codeflyingUserId) {
        String key = USER_CODEFLYING_MAPPING_KEY + codeflyingUserId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val != null) {
            return new BigDecimal(val.toString());
        }
        redisTemplate.opsForValue().set(key, DEFAULT_BALANCE.toPlainString());
        return DEFAULT_BALANCE;
    }

    private boolean isRedisEnabled() {
        return redisTemplate != null;
    }

    /**
     * 通知 backend 记录积分变动统计（消费/充值）
     * 失败只打 warn，不影响主流程
     */
    private void notifyBackendStat(Long userId, String type, BigDecimal amount) {
        if (backendUrl == null || backendUrl.isEmpty()) return;
        try {
            String url = backendUrl + "/balance_stat/record";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Key", backendAuthKey);
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("type", type);
            body.put("amount", amount);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("通知 backend 写入统计成功, userId: {}, type: {}, amount: {}", userId, type, amount);
        } catch (Exception e) {
            log.warn("通知 backend 写入统计失败, userId: {}, type: {}, amount: {}, error: {}", userId, type, amount, e.getMessage());
        }
    }
}
