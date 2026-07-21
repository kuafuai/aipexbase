package com.kuafuai.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.RlsPolicy;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.RlsPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RLS 策略管理. 只 owner 可访问.
 * <p>
 * 策略数据在主库 app_rls_policy 表, 由 PolicyEngine 在运行时按 (appId, tableName, operation) 查询注入到 SQL WHERE.
 */
@RestController
@RequestMapping("/admin/rls")
@RequiredArgsConstructor
@Slf4j
public class RlsPolicyController {

    private final RlsPolicyService rlsPolicyService;
    private final AppInfoService appInfoService;

    /**
     * GET /admin/rls/{appId}/policies?tableName=xxx
     * tableName 可选; 传了就只返回这张表的; 不传返回整个项目的.
     */
    @GetMapping("/{appId}/policies")
    public BaseResponse<?> list(@PathVariable String appId,
                                @RequestParam(required = false) String tableName) {
        checkAppPermission(appId);
        LambdaQueryWrapper<RlsPolicy> qw = new LambdaQueryWrapper<RlsPolicy>()
                .eq(RlsPolicy::getAppId, appId);
        if (tableName != null && !tableName.isEmpty()) {
            qw.eq(RlsPolicy::getTableName, tableName);
        }
        qw.orderByDesc(RlsPolicy::getPriority).orderByAsc(RlsPolicy::getId);
        List<RlsPolicy> list = rlsPolicyService.list(qw);
        return ResultUtils.success(list);
    }

    /**
     * POST /admin/rls/{appId}/policies
     * body: RlsPolicy (id 留空, appId 会覆盖为 path 上的)
     */
    @PostMapping("/{appId}/policies")
    public BaseResponse<?> create(@PathVariable String appId, @RequestBody RlsPolicy body) {
        checkAppPermission(appId);
        validate(body);

        body.setId(null);
        body.setAppId(appId);
        if (body.getEnabled() == null) body.setEnabled(true);
        if (body.getPriority() == null) body.setPriority(0);
        rlsPolicyService.save(body);
        return ResultUtils.success(body);
    }

    /**
     * PUT /admin/rls/{appId}/policies/{id}
     */
    @PutMapping("/{appId}/policies/{id}")
    public BaseResponse<?> update(@PathVariable String appId,
                                  @PathVariable Long id,
                                  @RequestBody RlsPolicy body) {
        checkAppPermission(appId);
        RlsPolicy existing = rlsPolicyService.getById(id);
        if (existing == null || !Objects.equals(existing.getAppId(), appId)) {
            throw new BusinessException("error.code.not_found");
        }
        validate(body);
        body.setId(id);
        body.setAppId(appId);
        rlsPolicyService.updateById(body);
        return ResultUtils.success(body);
    }

    /**
     * POST /admin/rls/{appId}/policies/{id}/toggle  { enabled: true|false }
     */
    @PostMapping("/{appId}/policies/{id}/toggle")
    public BaseResponse<?> toggle(@PathVariable String appId,
                                  @PathVariable Long id,
                                  @RequestBody Map<String, Object> body) {
        checkAppPermission(appId);
        RlsPolicy existing = rlsPolicyService.getById(id);
        if (existing == null || !Objects.equals(existing.getAppId(), appId)) {
            throw new BusinessException("error.code.not_found");
        }
        Boolean enabled = (Boolean) body.getOrDefault("enabled", Boolean.TRUE);
        rlsPolicyService.togglePolicy(id, enabled);
        return ResultUtils.success();
    }

    /**
     * DELETE /admin/rls/{appId}/policies/{id}
     */
    @DeleteMapping("/{appId}/policies/{id}")
    public BaseResponse<?> delete(@PathVariable String appId, @PathVariable Long id) {
        checkAppPermission(appId);
        RlsPolicy existing = rlsPolicyService.getById(id);
        if (existing == null || !Objects.equals(existing.getAppId(), appId)) {
            throw new BusinessException("error.code.not_found");
        }
        rlsPolicyService.removeById(id);
        return ResultUtils.success();
    }

    private void validate(RlsPolicy p) {
        if (isBlank(p.getTableName())) throw new BusinessException("error.param.required", "tableName");
        if (isBlank(p.getPolicyName())) throw new BusinessException("error.param.required", "policyName");
        if (isBlank(p.getOperation())) throw new BusinessException("error.param.required", "operation");
        String op = p.getOperation().toUpperCase();
        if (!op.equals("SELECT") && !op.equals("INSERT") && !op.equals("UPDATE")
                && !op.equals("DELETE") && !op.equals("ALL")) {
            throw new BusinessException("error.param.invalid", "operation");
        }
        p.setOperation(op);
        // SELECT/DELETE 只需 USING; INSERT 只需 WITH CHECK; UPDATE/ALL 两个都要
        if (op.equals("SELECT") || op.equals("DELETE") || op.equals("UPDATE") || op.equals("ALL")) {
            if (isBlank(p.getUsingExpression())) throw new BusinessException("error.param.required", "usingExpression");
        }
        if (op.equals("INSERT") || op.equals("UPDATE") || op.equals("ALL")) {
            // WITH CHECK 允许空 (INSERT/UPDATE 无限制)
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void checkAppPermission(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("error.code.not_found");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }
    }
}
