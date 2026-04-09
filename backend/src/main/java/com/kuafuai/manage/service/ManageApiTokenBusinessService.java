package com.kuafuai.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.context.ManageApiContext;
import com.kuafuai.system.entity.ManageApiToken;
import com.kuafuai.system.service.ManageApiTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * 管理API Token业务服务
 * 负责Token的验证、创建、管理等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageApiTokenBusinessService {

    private final ManageApiTokenService manageApiTokenService;

    /**
     * 验证Token并构建上下文
     *
     * @param token          Token字符串
     * @param externalUserId 外部用户ID（场景2需要传入）
     * @return 上下文信息，验证失败返回null
     */
    public ManageApiContext validateTokenAndBuildContext(String token, String externalUserId) {
        // 1. 查询token记录
        ManageApiToken tokenRecord = manageApiTokenService.getByToken(token);

        if (tokenRecord == null) {
            log.warn("Token不存在: {}", token);
            return null;
        }

        // 2. 检查状态
        if (ManageApiToken.Status.DISABLED == tokenRecord.getStatus()) {
            log.warn("Token已禁用: tokenId={}", tokenRecord.getId());
            return null;
        }

        // 3. 检查过期时间
        if (tokenRecord.getExpireTime() != null && tokenRecord.getExpireTime().before(new Date())) {
            log.warn("Token已过期: tokenId={}, expireTime={}", tokenRecord.getId(), tokenRecord.getExpireTime());
            return null;
        }

        // 4. 根据token类型分别处理
        ManageApiContext context = new ManageApiContext();
        context.setTokenId(tokenRecord.getId());
        context.setTokenType(tokenRecord.getTokenType());
        context.setTokenName(tokenRecord.getName());

        if (ManageApiToken.TokenType.USER.equals(tokenRecord.getTokenType())) {
            // 场景1：用户级Token（userId固化在token中）

            if (StringUtils.isEmpty(tokenRecord.getUserId())) {
                log.error("用户级Token缺少userId: tokenId={}", tokenRecord.getId());
                return null;
            }

            context.setUserId(tokenRecord.getUserId());
        } else if (ManageApiToken.TokenType.COMPANY.equals(tokenRecord.getTokenType())) {
            // 场景2：企业级Token

            context.setCompanyId(tokenRecord.getCompanyId());
            context.setUserId(externalUserId);
        } else {
            log.error("未知的Token类型: type={}", tokenRecord.getTokenType());
            return null;
        }

        // 5. 异步更新最后使用时间
        try {
            manageApiTokenService.updateLastUsedTime(tokenRecord.getId());
        } catch (Exception e) {
            log.error("更新Token最后使用时间失败: tokenId={}", tokenRecord.getId(), e);
        }

        return context;
    }

    /**
     * 创建用户级Token
     *
     * @param userId 外部用户ID（任意字符串）
     */
    public ManageApiToken createUserToken(String userId, String name, String remark, Date expireTime) {
        if (StringUtils.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        ManageApiToken token = ManageApiToken.builder()
                .token(generateToken())
                .tokenType(ManageApiToken.TokenType.USER)
                .name(name)
                .userId(userId)
                .status(ManageApiToken.Status.ENABLED)
                .remark(remark)
                .build();

        manageApiTokenService.save(token);
        log.info("创建用户级Token成功: tokenId={}, userId={}", token.getId(), userId);
        return token;
    }

    /**
     * 创建企业级Token
     */
    public ManageApiToken createCompanyToken(String companyId, String companyName, String name, String remark, Date expireTime) {
        if (companyId == null || companyId.trim().isEmpty()) {
            throw new BusinessException("企业ID不能为空");
        }

        ManageApiToken token = ManageApiToken.builder()
                .token(generateToken())
                .tokenType(ManageApiToken.TokenType.COMPANY)
                .name(name)
                .companyId(companyId)
                .companyName(companyName)
                .status(ManageApiToken.Status.ENABLED)
                .remark(remark)
                .build();

        manageApiTokenService.save(token);
        log.info("创建企业级Token成功: tokenId={}, companyId={}", token.getId(), companyId);
        return token;
    }

    /**
     * 查询Token列表（分页）
     */
    public IPage<ManageApiToken> listTokens(int pageNum, int pageSize, String tokenType, String userId, String companyId) {
        Page<ManageApiToken> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ManageApiToken> wrapper = new LambdaQueryWrapper<>();

        if (tokenType != null && !tokenType.isEmpty()) {
            wrapper.eq(ManageApiToken::getTokenType, tokenType);
        }
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(ManageApiToken::getUserId, userId);
        }
        if (companyId != null && !companyId.isEmpty()) {
            wrapper.eq(ManageApiToken::getCompanyId, companyId);
        }

        wrapper.orderByDesc(ManageApiToken::getCreateTime);
        return manageApiTokenService.page(page, wrapper);
    }

    /**
     * 根据ID获取Token
     */
    public ManageApiToken getTokenById(Long id) {
        return manageApiTokenService.getById(id);
    }

    /**
     * 启用/禁用Token
     */
    public void updateTokenStatus(Long id, Integer status) {
        ManageApiToken token = manageApiTokenService.getById(id);
        if (token == null) {
            throw new BusinessException("Token不存在");
        }

        token.setStatus(status);
        token.setUpdateTime(new Date());
        manageApiTokenService.updateById(token);
        log.info("更新Token状态成功: tokenId={}, status={}", id, status);
    }

    /**
     * 删除Token
     */
    public void deleteToken(Long id) {
        ManageApiToken token = manageApiTokenService.getById(id);
        if (token == null) {
            throw new BusinessException("Token不存在");
        }

        manageApiTokenService.removeById(id);
        log.info("删除Token成功: tokenId={}", id);
    }

    /**
     * 生成Token字符串
     */
    private String generateToken() {
        return "mgt_" + UUID.randomUUID().toString().replace("-", "");
    }
}
