package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.policy.RlsPolicyParser;
import com.kuafuai.system.entity.RlsPolicy;
import com.kuafuai.system.mapper.RlsPolicyMapper;
import com.kuafuai.system.service.RlsPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * RLS 策略服务实现
 */
@Slf4j
@Service
public class RlsPolicyServiceImpl extends ServiceImpl<RlsPolicyMapper, RlsPolicy> implements RlsPolicyService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RlsPolicy createFromSql(String appId, String sql, Integer priority) {
        if (StringUtils.isEmpty(appId)) {
            throw new BusinessException("policy.app_id.required");
        }

        if (StringUtils.isEmpty(sql)) {
            throw new BusinessException("policy.sql.required");
        }

        // 解析 SQL 语句
        RlsPolicyParser.ParsedPolicy parsed = RlsPolicyParser.parse(sql);

        // 检查策略名是否已存在
        RlsPolicy existing = getByPolicyName(appId, parsed.getTableName(), parsed.getPolicyName());
        if (existing != null) {
            log.warn("策略名已存在: appId={}, table={}, policy={}", appId, parsed.getTableName(), parsed.getPolicyName());
            throw new BusinessException("policy.name.duplicate");
        }

        // 构建策略对象
        RlsPolicy policy = RlsPolicy.builder()
                .appId(appId)
                .tableName(parsed.getTableName())
                .policyName(parsed.getPolicyName())
                .operation(parsed.getOperation())
                .usingExpression(parsed.getUsingExpression())
                .withCheckExpression(parsed.getWithCheckExpression())
                .enabled(true)
                .priority(priority != null ? priority : 0)
                .description("Created from SQL: " + sql)
                .build();

        // 保存到数据库
        save(policy);

        log.info("RLS 策略创建成功: id={}, appId={}, table={}, policy={}",
                policy.getId(), appId, parsed.getTableName(), parsed.getPolicyName());

        return policy;
    }

    @Override
    public List<RlsPolicy> getPoliciesByTable(String appId, String tableName) {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(tableName)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<RlsPolicy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RlsPolicy::getAppId, appId)
                .eq(RlsPolicy::getTableName, tableName)
                .eq(RlsPolicy::getEnabled, true)
                .orderByDesc(RlsPolicy::getPriority)
                .orderByAsc(RlsPolicy::getId);

        return list(queryWrapper);
    }

    @Override
    public List<RlsPolicy> getPoliciesByTableAndOperation(String appId, String tableName, String operation) {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(tableName)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<RlsPolicy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RlsPolicy::getAppId, appId)
                .eq(RlsPolicy::getTableName, tableName)
                .eq(RlsPolicy::getEnabled, true)
                .and(wrapper -> wrapper
                        .eq(RlsPolicy::getOperation, operation)
                        .or()
                        .eq(RlsPolicy::getOperation, "ALL")
                )
                .orderByDesc(RlsPolicy::getPriority)
                .orderByAsc(RlsPolicy::getId);

        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean togglePolicy(Long policyId, Boolean enabled) {
        if (policyId == null) {
            throw new BusinessException("policy.id.required");
        }

        if (enabled == null) {
            throw new BusinessException("policy.enabled.required");
        }

        LambdaUpdateWrapper<RlsPolicy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RlsPolicy::getId, policyId)
                .set(RlsPolicy::getEnabled, enabled);

        boolean success = update(updateWrapper);

        if (success) {
            log.info("RLS 策略状态已更新: id={}, enabled={}", policyId, enabled);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByTable(String appId, String tableName) {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(tableName)) {
            throw new BusinessException("policy.params.required");
        }

        LambdaQueryWrapper<RlsPolicy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RlsPolicy::getAppId, appId)
                .eq(RlsPolicy::getTableName, tableName);

        int count = (int) count(queryWrapper);
        remove(queryWrapper);

        log.info("删除表的所有 RLS 策略: appId={}, table={}, count={}", appId, tableName, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByAppId(String appId) {
        if (StringUtils.isEmpty(appId)) {
            throw new BusinessException("policy.app_id.required");
        }

        LambdaQueryWrapper<RlsPolicy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RlsPolicy::getAppId, appId);
        
        return remove(queryWrapper);
    }

    /**
     * 根据策略名查询策略
     */
    private RlsPolicy getByPolicyName(String appId, String tableName, String policyName) {
        LambdaQueryWrapper<RlsPolicy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RlsPolicy::getAppId, appId)
                .eq(RlsPolicy::getTableName, tableName)
                .eq(RlsPolicy::getPolicyName, policyName);

        return getOne(queryWrapper);
    }
}
