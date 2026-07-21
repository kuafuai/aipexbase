package com.kuafuai.manage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 项目终端用户管理 (login 表). 只 owner 可访问.
 * <p>
 * 复用 DynamicInterfaceService 走跟前端 Table Editor 完全同一套读写路径,
 * 不引入独立 mapper. 敏感字段 password 一律 mask 后再返回.
 */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthUsersController {

    private static final String TABLE_LOGIN = "login";
    private static final String PK = "login_id";

    private final DynamicInterfaceService dynamicService;
    private final AppInfoService appInfoService;
    private final SystemBusinessService systemBusinessService;

    /**
     * GET /admin/auth/{appId}/users?current=1&pageSize=20&keyword=xxx
     * keyword 按 user_name 模糊搜索 (dslType=keyword 会自动 LIKE).
     */
    @GetMapping("/{appId}/users")
    public BaseResponse<?> listUsers(@PathVariable String appId,
                                     @RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "20") long pageSize,
                                     @RequestParam(required = false) String keyword) {
        checkAppPermission(appId);
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        if (current <= 0) current = 1;

        Map<String, Object> conditions = Maps.newHashMap();
        conditions.put("current", current);
        conditions.put("pageSize", pageSize);
        conditions.put("order_by", PK + " DESC");
        if (StringUtils.isNotEmpty(keyword)) {
            conditions.put("user_name", keyword);
        }

        Page page = dynamicService.page(appId, TABLE_LOGIN, conditions);
        // mask password on every row
        List<Map<String, Object>> records = page.getRecords();
        if (records != null) {
            records.forEach(r -> r.remove("password"));
        }
        return ResultUtils.success(page);
    }

    /**
     * GET /admin/auth/{appId}/users/{loginId} 单个用户详情 (password mask).
     * 如果 login 行的 relevance_table + relevance_id 都有值, 会顺带把业务侧 profile join 出来
     * 放在 _profile 字段. 跟 LoginBusinessService.getCurrentUser() 同一套路数.
     */
    @GetMapping("/{appId}/users/{loginId}")
    public BaseResponse<?> getUser(@PathVariable String appId, @PathVariable Integer loginId) {
        checkAppPermission(appId);
        Map<String, Object> conditions = Maps.newHashMap();
        conditions.put(PK, loginId);
        Map<String, Object> row = dynamicService.get(appId, TABLE_LOGIN, conditions);
        if (row == null) {
            throw new BusinessException("error.code.not_found");
        }
        row.remove("password");

        // 尝试拉业务档案. 失败不阻塞主流程 —— 业务表可能已删/未注册/权限问题.
        Map<String, Object> profile = tryLoadProfile(appId, row);
        if (profile != null) {
            profile.remove("password"); // 保险起见, 业务表如果有 password 列也 mask
            row.put("_profile", profile);
        }
        return ResultUtils.success(row);
    }

    private Map<String, Object> tryLoadProfile(String appId, Map<String, Object> loginRow) {
        try {
            Object rid = loginRow.get("relevance_id");
            String table = resolveRelevanceTable(appId);
            if (rid == null || StringUtils.isEmpty(table))
                return null;

            String pk = systemBusinessService.getAppTablePrimaryKey(appId, table);
            if (StringUtils.isEmpty(pk))
                return null;

            Map<String, Object> q = Maps.newHashMap();
            q.put(pk, rid);
            return dynamicService.get(appId, table, q);
        } catch (Exception e) {
            log.debug("load profile skipped for {}: {}", loginRow.get(PK), e.getMessage());
            return null;
        }
    }

    /**
     * 优先用 login 上的 relevance_table, 空则回落到 AppInfo.authTable.
     */
    private String resolveRelevanceTable(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        return appInfo == null ? null : appInfo.getAuthTable();
    }

    /**
     * DELETE /admin/auth/{appId}/users/{loginId}
     */
    @DeleteMapping("/{appId}/users/{loginId}")
    public BaseResponse<?> deleteUser(@PathVariable String appId, @PathVariable Integer loginId) {
        checkAppPermission(appId);
        Map<String, Object> conditions = Maps.newHashMap();
        conditions.put(PK, loginId);
        int affected = dynamicService.delete(appId, TABLE_LOGIN, conditions);
        if (affected == 0) {
            throw new BusinessException("error.code.not_found");
        }
        return ResultUtils.success();
    }

    /**
     * POST /admin/auth/{appId}/users/{loginId}/reset-password
     * <p>
     * 生成 12 位随机明文, BCrypt 存入 login.password, 明文一次性返回 (前端弹窗显示).
     */
    @PostMapping("/{appId}/users/{loginId}/reset-password")
    public BaseResponse<?> resetPassword(@PathVariable String appId, @PathVariable Integer loginId) {
        checkAppPermission(appId);

        // 先确认存在
        Map<String, Object> query = Maps.newHashMap();
        query.put(PK, loginId);
        Map<String, Object> existing = dynamicService.get(appId, TABLE_LOGIN, query);
        if (existing == null) {
            throw new BusinessException("error.code.not_found");
        }

        String plaintext = generateRandomPassword(12);
        String encoded = SecurityUtils.encryptPassword(plaintext);

        Map<String, Object> update = Maps.newHashMap();
        update.put(PK, loginId);
        update.put("password", encoded);
        dynamicService.update(appId, TABLE_LOGIN, update);

        Map<String, Object> data = Maps.newHashMap();
        data.put("password", plaintext);
        return ResultUtils.success(data);
    }

    /**
     * 12 位 [A-Za-z0-9], 去掉易混字符.
     */
    private String generateRandomPassword(int length) {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        }
        return sb.toString();
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
