package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.system.entity.PointsRecord;
import com.kuafuai.system.entity.UserBalance;
import com.kuafuai.system.entity.Users;
import com.kuafuai.system.mapper.UserBalanceMapper;
import com.kuafuai.system.service.BalancePackageService;
import com.kuafuai.system.service.PointsRecordService;
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
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class UserBalanceServiceImpl extends ServiceImpl<UserBalanceMapper, UserBalance> implements UserBalanceService {

    // ── Redis key 前缀 ──────────────────────────────────────────
    private static final String BALANCE_KEY = "user:codeflying:balance:";
    private static final String DAILY_POINTS_KEY = "user:daily_points:";
    private static final String DAILY_DATE_KEY = "user:daily_points:date:";
    private static final String INVITE_POINTS_KEY = "user:invite_points:";
    private static final String MONTHLY_POINTS_KEY = "user:monthly_points:";
    private static final String APP_LIMIT_KEY = "user:app_limit_reached:";

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("2.00");

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${backend.url:}")
    private String backendUrl;

    @Value("${backend.internal.auth-key:b54igLGJ1DpB8OMF}")
    private String backendAuthKey;

    @Resource
    private UsersService usersService;

    @Resource
    private PointsRecordService pointsRecordService;

    @Resource
    private BalancePackageService balancePackageService;

    @Resource
    private DefaultDbHelper defaultDbHelper;

    @Autowired(required = false)
    @Qualifier(value = "dataRouterRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    // ══════════════════════════════════════════════════════════════
    // 查询
    // ══════════════════════════════════════════════════════════════

    @Override
    public UserBalance getByUserId(Long userId) {
        log.info("开始查询用户余额，用户ID: {}", userId);
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            UserBalance userBalance = defaultDbHelper.getByUserId(userId);
            log.info("用户余额查询完成，用户ID: {}，查询结果: {}", userId, userBalance);
            return userBalance;
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
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

    // ══════════════════════════════════════════════════════════════
    // 余额检查：三类任一 > 0 即通过
    // ══════════════════════════════════════════════════════════════

    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        log.info("开始检查用户余额是否充足，用户ID: {}，需检查金额: {}", userId, amount);
        try {
            Users user = usersService.getById(userId);
            if (user != null && user.getCodeFlyingUserId() != null) {
                return checkAllPoints(user.getCodeFlyingUserId(), amount);
            }
        } catch (Exception e) {
            log.error("检查用户余额异常, userId: {}", userId, e);
        }
        return false;
    }

    @Override
    public boolean checkBalanceByCodeFlyingUserId(String codeFlyingUserId) {
        return checkAllPoints(codeFlyingUserId, BigDecimal.ZERO);
    }

    /**
     * 检查四类积分是否有任一余额 > 0
     */
    private boolean checkAllPoints(String codeFlyingUserId, BigDecimal amount) {
        log.info("[checkAllPoints] 开始, codeFlyingUserId: {}, amount: {}, redisEnabled: {}",
                codeFlyingUserId, amount, isRedisEnabled());

        if (!isRedisEnabled()) {
            Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
            if (user == null) return false;
            UserBalance ub = getByUserId(user.getId());
            if (ub == null || ub.getStatus() != 1) return false;
            return ub.getBalance().compareTo(amount) >= 0;
        }

        try {
            redisTemplate.delete(DAILY_DATE_KEY + codeFlyingUserId);
            // 确保每日/月积分已初始化（每天同步一次 kuafu）
            ensurePointsInitialized(codeFlyingUserId);

            // 检查应用数量是否到达上限
            if (isAppLimitReached(codeFlyingUserId)) {
                log.warn("应用数量已达上限, codeFlyingUserId: {}", codeFlyingUserId);
                return false;
            }

            // 任一类有余额即通过
            int daily = getDailyPoints(codeFlyingUserId);
            int invite = getInvitePoints(codeFlyingUserId);
            int monthly = getMonthlyPoints(codeFlyingUserId);
            BigDecimal balance = fetchCurrentBalance(codeFlyingUserId);

            log.info("[checkAllPoints] codeFlyingUserId: {}, daily: {}, invite: {}, monthly: {}, balance: {}",
                    codeFlyingUserId, daily, invite, monthly, balance);

            if (daily > 0) {
                log.info("每日积分充足, codeFlyingUserId: {}, daily: {}", codeFlyingUserId, daily);
                return true;
            }
            if (invite > 0) {
                log.info("邀请积分充足, codeFlyingUserId: {}, invite: {}", codeFlyingUserId, invite);
                return true;
            }
            if (monthly > 0) {
                log.info("月积分充足, codeFlyingUserId: {}, monthly: {}", codeFlyingUserId, monthly);
                return true;
            }

            boolean sufficient = balance.compareTo(amount) >= 0;
            log.info("加油包检查, codeFlyingUserId: {}, balance: {}, amount: {}, sufficient: {}",
                    codeFlyingUserId, balance, amount, sufficient);
            return sufficient;

        } catch (Exception e) {
            log.error("checkAllPoints 异常, codeFlyingUserId: {}", codeFlyingUserId, e);
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 扣减：优先级 每日赠送 → 月积分 → 加油包
    // ══════════════════════════════════════════════════════════════

    @Override
    public boolean deductBalance(Long userId, BigDecimal amount) {
        log.info("开始扣减用户余额，用户ID: {}，扣减金额: {}", userId, amount);
        try {
            Users user = usersService.getById(userId);
            if (user == null || user.getCodeFlyingUserId() == null) {
                log.error("用户不存在, userId: {}", userId);
                return false;
            }
            return deductWithPriority(user.getCodeFlyingUserId(), amount);
        } catch (Exception e) {
            log.error("扣减余额异常, userId: {}, amount: {}", userId, amount, e);
            return false;
        }
    }

    @Override
    public boolean deductBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount) {
        log.info("开始扣减用户余额, codeFlyingUserId: {}, 扣减金额: {}", codeFlyingUserId, amount);
        try {
            return deductWithPriority(codeFlyingUserId, amount);
        } catch (Exception e) {
            log.error("扣减余额异常, codeFlyingUserId: {}, amount: {}", codeFlyingUserId, amount, e);
            return false;
        }
    }

    /**
     * 核心：按优先级扣减 每日赠送 → 邀请积分 → 月积分 → 加油包。
     * 支持跨层级扣减：如每日剩余 2 但要扣 5，则从每日扣 2 + 月积分扣 3。
     */
    private boolean deductWithPriority(String codeFlyingUserId, BigDecimal amount) {
        if (!isRedisEnabled()) {
            Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
            if (user == null) return false;
            String prev = DynamicDataSourceContextHolder.getDataSourceType();
            DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
            try {
                int rows = defaultDbHelper.forceDeductBalance(user.getId(), amount);
                if (rows > 0) {
                    savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_BALANCE, amount.multiply(BigDecimal.TEN));
                }
                return rows > 0;
            } finally {
                DynamicDataSourceContextHolder.setDataSourceType(prev);
            }
        }

        // 扣减前清除缓存日期标记，强制从 kuafu 获取最新数据
        redisTemplate.delete(DAILY_DATE_KEY + codeFlyingUserId);

        // 从 kuafu 同步最新积分数据
        ensurePointsInitialized(codeFlyingUserId);

        BigDecimal deductAmount = amount.multiply(BigDecimal.TEN);
        BigDecimal remaining = deductAmount;
        Long cfUserId = Long.parseLong(codeFlyingUserId);

        // 1. 先从每日积分扣（不够的部分留给下一层）
        BigDecimal daily = toDecimal(redisTemplate.opsForValue().get(DAILY_POINTS_KEY + codeFlyingUserId));
        if (daily.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deduct = daily.min(remaining);
            BigDecimal newDaily = daily.subtract(deduct);
            redisTemplate.opsForValue().set(DAILY_POINTS_KEY + codeFlyingUserId, newDaily.toPlainString());
            remaining = remaining.subtract(deduct);
            log.info("每日积分扣减: codeFlyingUserId={}, 扣={}, 剩余daily={}", codeFlyingUserId, deduct, newDaily);
            savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_DAILY, deduct);
            notifyBackendSync(cfUserId, "DAILY", deduct);
        }
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return true;

        // 2. 再从邀请积分扣
        BigDecimal invite = toDecimal(redisTemplate.opsForValue().get(INVITE_POINTS_KEY + codeFlyingUserId));
        if (invite.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deduct = invite.min(remaining);
            BigDecimal newInvite = invite.subtract(deduct);
            redisTemplate.opsForValue().set(INVITE_POINTS_KEY + codeFlyingUserId, newInvite.toPlainString());
            remaining = remaining.subtract(deduct);
            log.info("邀请积分扣减: codeFlyingUserId={}, 扣={}, 剩余invite={}", codeFlyingUserId, deduct, newInvite);
            savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_INVITATION, deduct);
            notifyBackendSync(cfUserId, "INVITATION", deduct);
        }
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return true;

        // 3. 再从月积分扣
        BigDecimal monthly = toDecimal(redisTemplate.opsForValue().get(MONTHLY_POINTS_KEY + codeFlyingUserId));
        if (monthly.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal deduct = monthly.min(remaining);
            BigDecimal newMonthly = monthly.subtract(deduct);
            redisTemplate.opsForValue().set(MONTHLY_POINTS_KEY + codeFlyingUserId, newMonthly.toPlainString());
            remaining = remaining.subtract(deduct);
            log.info("月积分扣减: codeFlyingUserId={}, 扣={}, 剩余monthly={}", codeFlyingUserId, deduct, newMonthly);
            savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_MONTHLY, deduct);
            notifyBackendSync(cfUserId, "MONTHLY", deduct);
        }
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return true;

        // 4. 剩余部分从加油包扣（按过期时间 FIFO，自动清理过期包）
        BigDecimal balanceRemaining = remaining.divide(BigDecimal.TEN, 6, BigDecimal.ROUND_HALF_UP);
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        BigDecimal deducted;
        try {
            deducted = balancePackageService.deductFromPackages(codeFlyingUserId, balanceRemaining);
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
        if (deducted.compareTo(balanceRemaining) < 0) {
            log.warn("加油包余额不足, codeFlyingUserId={}, 需扣={}, 实际扣={}",
                    codeFlyingUserId, balanceRemaining, deducted);
            // 已扣到0，同步缓存后返回失败
            if (isRedisEnabled()) {
                redisTemplate.opsForValue().set(BALANCE_KEY + codeFlyingUserId, "0");
            }
            savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_BALANCE, deducted.multiply(BigDecimal.TEN));
            notifyBackendStat(cfUserId, "consume", deducted);
            return false;
        }

        // 同步 Redis 缓存
        if (isRedisEnabled()) {
            String prev2 = DynamicDataSourceContextHolder.getDataSourceType();
            DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
            try {
                BigDecimal validBalance = balancePackageService.getValidBalance(codeFlyingUserId);
                redisTemplate.opsForValue().set(BALANCE_KEY + codeFlyingUserId, validBalance.toPlainString());
            } finally {
                DynamicDataSourceContextHolder.setDataSourceType(prev2);
            }
        }
        log.info("加油包扣减: codeFlyingUserId={}, 扣={}", codeFlyingUserId, balanceRemaining);
        savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_DEDUCT, PointsRecord.SUB_BALANCE, remaining);
        notifyBackendStat(cfUserId, "consume", balanceRemaining);
        return true;
    }

    // ══════════════════════════════════════════════════════════════
    // 充值
    // ══════════════════════════════════════════════════════════════

    @Override
    public boolean increaseBalanceByCodeFlyingUserId(String codeFlyingUserId, BigDecimal amount, String rechargeType) {
        try {
            // 写入加油包明细（1年有效期）
            balancePackageService.addPackage(codeFlyingUserId, amount, rechargeType);

            // 更新 Redis 缓存余额
            if (isRedisEnabled()) {
                BigDecimal validBalance = balancePackageService.getValidBalance(codeFlyingUserId);
                redisTemplate.opsForValue().set(BALANCE_KEY + codeFlyingUserId, validBalance.toPlainString());
                log.info("加油包充值成功, codeFlyingUserId: {}, 充值: {}, 有效总余额: {}, type: {}",
                        codeFlyingUserId, amount, validBalance, rechargeType);
            } else {
                Users user = usersService.getByCodeFlyingUserId(codeFlyingUserId);
                String prev = DynamicDataSourceContextHolder.getDataSourceType();
                DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
                try {
                    defaultDbHelper.increaseBalance(user.getId(), amount);
                } finally {
                    DynamicDataSourceContextHolder.setDataSourceType(prev);
                }
                log.info("DB余额增加成功, codeFlyingUserId: {}, 增加金额: {}, type: {}", codeFlyingUserId, amount, rechargeType);
            }

            // 积分记录的值 * 10
            savePointsRecord(codeFlyingUserId, PointsRecord.TYPE_RECHARGE, rechargeType, amount.multiply(BigDecimal.TEN));
            notifyBackendStat(Long.parseLong(codeFlyingUserId), "recharge", amount);
            return true;

        } catch (Exception e) {
            log.error("增加用户余额异常, codeFlyingUserId: {}, amount: {}", codeFlyingUserId, amount, e);
            return false;
        }
    }


    // ══════════════════════════════════════════════════════════════
    // 每日/月积分 Redis 管理
    // ══════════════════════════════════════════════════════════════

    /**
     * 确保每日积分和月积分都已初始化（懒加载）。
     * 每日积分：按自然日重置，额度 = daily_quota 配置值。
     * 月积分：从 kuafu 获取 reset 后的实际剩余（31天周期由 kuafu 管理），
     *         每天同步一次保证拿到最新值。
     */
    private void ensurePointsInitialized(String codeFlyingUserId) {
        String dateKey = DAILY_DATE_KEY + codeFlyingUserId;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        Object storedDate = redisTemplate.opsForValue().get(dateKey);
        log.info("[ensurePoints] codeFlyingUserId: {}, dateKey存储值: {}, today: {}",
                codeFlyingUserId, storedDate, today);

        if (today.equals(storedDate != null ? storedDate.toString() : "")) {
            log.info("[ensurePoints] 今天已初始化，跳过, codeFlyingUserId: {}", codeFlyingUserId);
            return;
        }

        log.info("[ensurePoints] 需要初始化, codeFlyingUserId: {}, backendUrl: {}",
                codeFlyingUserId, backendUrl);

        // 新的一天：调 kuafu 获取最新配额（含实际剩余值，kuafu 已执行懒加载赠送）
        Map<String, Object> config = fetchQuotaConfig(Long.parseLong(codeFlyingUserId));

        if (config == null) {
            log.warn("[ensurePoints] 获取配额失败，跳过初始化，下次重试, codeFlyingUserId: {}", codeFlyingUserId);
            return;
        }

        log.info("[ensurePoints] kuafu返回配额: codeFlyingUserId: {}, config: {}", codeFlyingUserId, config);

        int dailyRemaining = toInt(config.get("daily_remaining"));
        int inviteRemaining = toInt(config.get("invite_remaining"));
        int monthlyRemaining = toInt(config.get("monthly_remaining"));
        boolean appLimitReached = Boolean.TRUE.equals(config.get("app_limit_reached"));

        // 同步每日积分实际剩余（kuafu 已执行 get_or_create_today）
        redisTemplate.opsForValue().set(DAILY_POINTS_KEY + codeFlyingUserId, String.valueOf(dailyRemaining));

        // 同步邀请积分剩余
        redisTemplate.opsForValue().set(INVITE_POINTS_KEY + codeFlyingUserId, String.valueOf(inviteRemaining));

        // 同步月积分剩余（kuafu 已执行 reset_monthly_count）
        redisTemplate.opsForValue().set(MONTHLY_POINTS_KEY + codeFlyingUserId, String.valueOf(monthlyRemaining));

        // 同步应用数量是否到达上限
        redisTemplate.opsForValue().set(APP_LIMIT_KEY + codeFlyingUserId, String.valueOf(appLimitReached));

        // 同步加油包有效余额（清理过期包后重算）
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        BigDecimal validBalance;
        try {
            validBalance = balancePackageService.getValidBalance(codeFlyingUserId);
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
        redisTemplate.opsForValue().set(BALANCE_KEY + codeFlyingUserId, validBalance.toPlainString());

        // 标记今天已初始化
        redisTemplate.opsForValue().set(dateKey, today);

        log.info("积分已初始化, codeFlyingUserId: {}, daily: {}, invite: {}, monthly: {}, balance: {}, date: {}",
                codeFlyingUserId, dailyRemaining, inviteRemaining, monthlyRemaining, validBalance, today);
    }

    private int getDailyPoints(String codeFlyingUserId) {
        return toInt(redisTemplate.opsForValue().get(DAILY_POINTS_KEY + codeFlyingUserId));
    }

    private int getInvitePoints(String codeFlyingUserId) {
        return toInt(redisTemplate.opsForValue().get(INVITE_POINTS_KEY + codeFlyingUserId));
    }

    private int getMonthlyPoints(String codeFlyingUserId) {
        return toInt(redisTemplate.opsForValue().get(MONTHLY_POINTS_KEY + codeFlyingUserId));
    }

    private boolean isAppLimitReached(String codeFlyingUserId) {
        Object val = redisTemplate.opsForValue().get(APP_LIMIT_KEY + codeFlyingUserId);
        return val != null && "true".equals(val.toString());
    }

    // ══════════════════════════════════════════════════════════════
    // 私有工具方法
    // ══════════════════════════════════════════════════════════════

    private BigDecimal fetchCurrentBalance(String codeflyingUserId) {
        String key = BALANCE_KEY + codeflyingUserId;
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

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal toDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 保存积分变动记录到 DB（扣减/充值统一）
     */
    private void savePointsRecord(String codeFlyingUserId, String type, String subType, BigDecimal amount) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            pointsRecordService.saveToDefault(codeFlyingUserId, type, subType, amount);
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    /**
     * 调用 kuafu 获取用户配额配置（每日/月额度）
     */
    private Map<String, Object> fetchQuotaConfig(Long codeFlyingUserId) {
        if (backendUrl == null || backendUrl.isEmpty()) {
            log.warn("backendUrl 未配置，无法获取配额");
            return null;
        }

        try {
            String url = backendUrl + "/balance_stat/quota_config";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Key", backendAuthKey);

            Map<String, Object> body = new HashMap<>();
            body.put("user_id", codeFlyingUserId);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response != null && toInt(response.get("code")) == 0) {
                log.info("获取配额成功, codeFlyingUserId: {}, config: {}", codeFlyingUserId, response);
                return response;
            }
            log.warn("获取配额返回异常, codeFlyingUserId: {}, response: {}", codeFlyingUserId, response);
        } catch (Exception e) {
            log.warn("获取配额异常, codeFlyingUserId: {}, error: {}", codeFlyingUserId, e.getMessage());
        }
        return null;
    }

    /**
     * 通知 backend 同步扣减（每日/月积分）
     */
    private void notifyBackendSync(Long userId, String deductionType, BigDecimal amount) {
        if (backendUrl == null || backendUrl.isEmpty()) return;
        try {
            String url = backendUrl + "/balance_stat/sync_deduction";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Key", backendAuthKey);
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("deduction_type", deductionType);
            body.put("amount", amount);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("通知 backend 同步扣减成功, userId: {}, type: {}, amount: {}", userId, deductionType, amount);
        } catch (Exception e) {
            log.warn("通知 backend 同步扣减失败, userId: {}, type: {}, error: {}",
                    userId, deductionType, e.getMessage());
        }
    }

    /**
     * 通知 backend 记录积分变动统计（消费/充值）
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
            log.warn("通知 backend 写入统计失败, userId: {}, type: {}, amount: {}, error: {}",
                    userId, type, amount, e.getMessage());
        }
    }

    @Override
    public void invalidatePointsCache(String codeFlyingUserId) {
        if (!isRedisEnabled()) {
            log.info("Redis 未启用，跳过缓存清除, codeFlyingUserId: {}", codeFlyingUserId);
            return;
        }
        try {
            redisTemplate.delete(DAILY_DATE_KEY + codeFlyingUserId);
            redisTemplate.delete(DAILY_POINTS_KEY + codeFlyingUserId);
            redisTemplate.delete(MONTHLY_POINTS_KEY + codeFlyingUserId);
            redisTemplate.delete(INVITE_POINTS_KEY + codeFlyingUserId);
            redisTemplate.delete(APP_LIMIT_KEY + codeFlyingUserId);
            log.info("积分缓存已全部清除, codeFlyingUserId: {}", codeFlyingUserId);
        } catch (Exception e) {
            log.error("清除积分缓存失败, codeFlyingUserId: {}", codeFlyingUserId, e);
        }
    }
}
