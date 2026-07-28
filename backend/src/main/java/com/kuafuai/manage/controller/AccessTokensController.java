package com.kuafuai.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.AccessTokenCreatedVO;
import com.kuafuai.manage.entity.vo.AccessTokenMaskedVO;
import com.kuafuai.manage.service.ManageApiTokenBusinessService;
import com.kuafuai.system.entity.ManageApiToken;
import com.kuafuai.system.entity.Users;
import com.kuafuai.system.service.ManageApiTokenService;
import com.kuafuai.system.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 当前登录用户自管理其 Access Token (USER 类型).
 * 只允许创建/查看/管理自己的 token, 不涉及 COMPANY token (那是运营侧手工发放).
 */
@RestController
@RequestMapping("/admin/access-tokens")
@RequiredArgsConstructor
@Slf4j
public class AccessTokensController {

    private final ManageApiTokenService manageApiTokenService;
    private final ManageApiTokenBusinessService tokenBusinessService;
    private final UsersService usersService;

    /**
     * GET /admin/access-tokens
     * 我名下的所有 USER token, token 一律 mask.
     */
    @GetMapping
    public BaseResponse<?> list() {
        String userId = currentUserId();
        LambdaQueryWrapper<ManageApiToken> qw = new LambdaQueryWrapper<ManageApiToken>()
                .eq(ManageApiToken::getUserId, userId)
                .eq(ManageApiToken::getTokenType, ManageApiToken.TokenType.USER)
                .orderByDesc(ManageApiToken::getCreateTime);

        List<AccessTokenMaskedVO> vos = manageApiTokenService.list(qw).stream()
                .map(t -> AccessTokenMaskedVO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .remark(t.getRemark())
                        .tokenMasked(AccessTokenMaskedVO.mask(t.getToken()))
                        .status(t.getStatus())
                        .expireTime(t.getExpireTime())
                        .lastUsedTime(t.getLastUsedTime())
                        .createTime(t.getCreateTime())
                        .build())
                .collect(Collectors.toList());
        return ResultUtils.success(vos);
    }

    /**
     * POST /admin/access-tokens
     * body: { name, remark, expireInDays } expireInDays=null => 永不过期
     */
    @PostMapping
    public BaseResponse<?> create(@RequestBody Map<String, Object> body) {
        String userId = currentUserId();
        String name = strOrNull(body.get("name"));
        String remark = strOrNull(body.get("remark"));
        Integer expireInDays = intOrNull(body.get("expireInDays"));

        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("error.param.required", "name");
        }
        Date expireTime = null;
        if (expireInDays != null && expireInDays > 0) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, expireInDays);
            expireTime = c.getTime();
        }

        ManageApiToken t = tokenBusinessService.createUserToken(userId, name.trim(), remark, expireTime);
        return ResultUtils.success(AccessTokenCreatedVO.builder()
                .id(t.getId())
                .name(t.getName())
                .token(t.getToken())         // 一次性明文
                .expireTime(t.getExpireTime())
                .build());
    }

    /**
     * POST /admin/access-tokens/{id}/toggle  { enabled: true|false }
     */
    @PostMapping("/{id}/toggle")
    public BaseResponse<?> toggle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ManageApiToken token = requireOwnedToken(id);
        Boolean enabled = (Boolean) body.getOrDefault("enabled", Boolean.TRUE);
        tokenBusinessService.updateTokenStatus(id,
                enabled ? ManageApiToken.Status.ENABLED : ManageApiToken.Status.DISABLED);
        return ResultUtils.success();
    }

    /**
     * DELETE /admin/access-tokens/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<?> delete(@PathVariable Long id) {
        requireOwnedToken(id);
        tokenBusinessService.deleteToken(id);
        return ResultUtils.success();
    }

    // ---- helpers ----

    /**
     * 校验 token 存在且属于当前登录用户 (防止 A 用户传 B 用户的 id).
     */
    private ManageApiToken requireOwnedToken(Long id) {
        ManageApiToken token = manageApiTokenService.getById(id);
        if (token == null || !ManageApiToken.TokenType.USER.equals(token.getTokenType())
                || !Objects.equals(token.getUserId(), currentUserId())) {
            throw new BusinessException("error.code.not_found");
        }
        return token;
    }

    private String currentUserId() {
        Long uid = SecurityUtils.getUserId();
        if (uid == null) {
            throw new BusinessException("error.code.no_auth");
        }
        // 用 code_flying_user_id 作为跨产品身份, 让 manage API 侧的 getOrCreateUserByExternalId
        // 能通过这个值定位到本地 Users 行. 存量老用户可能是 NULL, 懒补一个.
        Users u = usersService.getById(uid);
        if (u == null) {
            throw new BusinessException("error.code.no_auth");
        }
        String cfUid = u.getCodeFlyingUserId();
        if (StringUtils.isEmpty(cfUid)) {
            cfUid = "aipex_" + u.getId();
            u.setCodeFlyingUserId(cfUid);
            usersService.updateById(u);
        }
        return cfUid;
    }

    private static String strOrNull(Object v) {
        return v == null ? null : v.toString();
    }

    private static Integer intOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
