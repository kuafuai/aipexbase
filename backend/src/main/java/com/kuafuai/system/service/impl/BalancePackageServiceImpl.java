package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.BalancePackage;
import com.kuafuai.system.mapper.BalancePackageMapper;
import com.kuafuai.system.service.BalancePackageService;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class BalancePackageServiceImpl extends ServiceImpl<BalancePackageMapper, BalancePackage> implements BalancePackageService {

    private static final int EXPIRE_DAYS = 365;
    private static final String BALANCE_KEY = "user:codeflying:balance:";
    private static final String MIGRATED_KEY = "user:balance:migrated:";

    @Autowired(required = false)
    @Qualifier(value = "dataRouterRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public BalancePackage addPackage(String codeFlyingUserId, BigDecimal amount, String source) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, EXPIRE_DAYS);

            BalancePackage pkg = BalancePackage.builder()
                    .codeFlyingUserId(codeFlyingUserId)
                    .totalAmount(amount)
                    .remainingAmount(amount)
                    .source(source)
                    .expiredAt(cal.getTime())
                    .status(BalancePackage.STATUS_ACTIVE)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
            save(pkg);
            log.info("新增加油包: userId={}, amount={}, source={}, expiredAt={}",
                    codeFlyingUserId, amount, source, pkg.getExpiredAt());
            return pkg;
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public BigDecimal deductFromPackages(String codeFlyingUserId, BigDecimal amount) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            ensureMigrated(codeFlyingUserId);
            cleanExpiredPackages(codeFlyingUserId);

            List<BalancePackage> packages = list(new LambdaQueryWrapper<BalancePackage>()
                    .eq(BalancePackage::getCodeFlyingUserId, codeFlyingUserId)
                    .eq(BalancePackage::getStatus, BalancePackage.STATUS_ACTIVE)
                    .gt(BalancePackage::getExpiredAt, new Date())
                    .orderByAsc(BalancePackage::getExpiredAt));

            BigDecimal remaining = amount;
            BigDecimal totalDeducted = BigDecimal.ZERO;

            for (BalancePackage pkg : packages) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal canDeduct = pkg.getRemainingAmount().min(remaining);
                if (canDeduct.compareTo(BigDecimal.ZERO) <= 0) continue;

                int rows = getBaseMapper().deductPackage(pkg.getId(), canDeduct);
                if (rows > 0) {
                    remaining = remaining.subtract(canDeduct);
                    totalDeducted = totalDeducted.add(canDeduct);
                    log.info("加油包扣减: pkgId={}, 扣={}, source={}, expiredAt={}",
                            pkg.getId(), canDeduct, pkg.getSource(), pkg.getExpiredAt());
                }
            }

            return totalDeducted;
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    @Override
    public Map<String, Object> getBalanceSummary(String codeFlyingUserId) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            ensureMigrated(codeFlyingUserId);
            cleanExpiredPackages(codeFlyingUserId);

            List<BalancePackage> packages = list(new LambdaQueryWrapper<BalancePackage>()
                    .eq(BalancePackage::getCodeFlyingUserId, codeFlyingUserId)
                    .eq(BalancePackage::getStatus, BalancePackage.STATUS_ACTIVE)
                    .gt(BalancePackage::getExpiredAt, new Date())
                    .orderByAsc(BalancePackage::getExpiredAt));

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal remaining = BigDecimal.ZERO;
            List<Map<String, Object>> details = new ArrayList<>();

            for (BalancePackage pkg : packages) {
                total = total.add(pkg.getTotalAmount());
                remaining = remaining.add(pkg.getRemainingAmount());
                Map<String, Object> detail = new HashMap<>();
                detail.put("amount", pkg.getTotalAmount());
                detail.put("remaining", pkg.getRemainingAmount());
                detail.put("expired_at", pkg.getExpiredAt());
                detail.put("source", pkg.getSource());
                details.add(detail);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("remaining", remaining);
            result.put("packages", details);
            return result;
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public BigDecimal getValidBalance(String codeFlyingUserId) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            ensureMigrated(codeFlyingUserId);
            cleanExpiredPackages(codeFlyingUserId);

            List<BalancePackage> packages = list(new LambdaQueryWrapper<BalancePackage>()
                    .eq(BalancePackage::getCodeFlyingUserId, codeFlyingUserId)
                    .eq(BalancePackage::getStatus, BalancePackage.STATUS_ACTIVE)
                    .gt(BalancePackage::getExpiredAt, new Date()));

            return packages.stream()
                    .map(BalancePackage::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    @Override
    public void cleanExpiredPackages(String codeFlyingUserId) {
        String prev = DynamicDataSourceContextHolder.getDataSourceType();
        DynamicDataSourceContextHolder.setDataSourceType("DEFAULT");
        try {
            int rows = getBaseMapper().expirePackages(codeFlyingUserId);
            if (rows > 0) {
                log.info("清理过期加油包: userId={}, 过期数={}", codeFlyingUserId, rows);
            }
        } finally {
            DynamicDataSourceContextHolder.setDataSourceType(prev);
        }
    }

    /**
     * 存量迁移：老用户有 Redis 余额但没有 balance_package 记录时，
     * 自动创建一条历史迁移包（source=MIGRATE，有效期1年）。
     * 通过 Redis 标记保证只迁移一次。
     */
    private void ensureMigrated(String codeFlyingUserId) {
        if (redisTemplate == null) return;

        String migratedKey = MIGRATED_KEY + codeFlyingUserId;
        Object migrated = redisTemplate.opsForValue().get(migratedKey);
        if (migrated != null) return; // 已迁移过

        try {
            // 检查是否有任何 balance_package 记录（含已过期的）
            long count = count(new LambdaQueryWrapper<BalancePackage>()
                    .eq(BalancePackage::getCodeFlyingUserId, codeFlyingUserId));
            if (count > 0) {
                redisTemplate.opsForValue().set(migratedKey, "1");
                return;
            }

            // 从 Redis 读当前余额
            Object val = redisTemplate.opsForValue().get(BALANCE_KEY + codeFlyingUserId);
            if (val == null) {
                redisTemplate.opsForValue().set(migratedKey, "1");
                return;
            }

            BigDecimal existingBalance = new BigDecimal(val.toString());
            if (existingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                redisTemplate.opsForValue().set(migratedKey, "1");
                return;
            }

            // 创建迁移包：total=remaining=当前余额，source=MIGRATE，1年有效
            addPackage(codeFlyingUserId, existingBalance, "MIGRATE");
            redisTemplate.opsForValue().set(migratedKey, "1");
            log.info("存量余额迁移: userId={}, balance={}", codeFlyingUserId, existingBalance);

        } catch (Exception e) {
            // DB 异常（如表不存在）不标记已迁移，下次重试
            log.warn("存量迁移异常，下次重试: userId={}, error={}", codeFlyingUserId, e.getMessage());
        }
    }
}
