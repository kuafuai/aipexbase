package com.kuafuai.manage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.manage.entity.dto.CreateTokenRequest;
import com.kuafuai.manage.service.ManageApiTokenBusinessService;
import com.kuafuai.system.entity.ManageApiToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理API Token管理接口
 * 用于后台管理员管理Token
 */
@Slf4j
@RestController
@RequestMapping("/admin/app/manage/token")
@RequiredArgsConstructor
public class ManageApiTokenController {

    private final ManageApiTokenBusinessService manageApiTokenBusinessService;

    /**
     * 创建用户级Token
     */
    @PostMapping("/user")
    public BaseResponse createUserToken(@RequestBody CreateTokenRequest request) {
        ManageApiToken token = manageApiTokenBusinessService.createUserToken(
                request.getUserId(),
                request.getName(),
                request.getRemark(),
                request.getExpireTime()
        );
        
        return ResultUtils.success(token);
    }

    /**
     * 创建企业级Token
     */
    @PostMapping("/company")
    public BaseResponse createCompanyToken(@RequestBody CreateTokenRequest request) {
        ManageApiToken token = manageApiTokenBusinessService.createCompanyToken(
                request.getCompanyId(),
                request.getCompanyName(),
                request.getName(),
                request.getRemark(),
                request.getExpireTime()
        );


        return ResultUtils.success(token);
    }

    /**
     * 查询Token列表
     */
    @GetMapping("/list")
    public BaseResponse listTokens(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String tokenType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String companyId
    ) {
        IPage<ManageApiToken> page = manageApiTokenBusinessService.listTokens(pageNum, pageSize, tokenType, userId, companyId);
        return ResultUtils.success(page);
    }

    /**
     * 获取Token详情
     */
    @GetMapping("/{id}")
    public BaseResponse getToken(@PathVariable Long id) {
        ManageApiToken token = manageApiTokenBusinessService.getTokenById(id);
        if (token == null) {
            return ResultUtils.error("Token不存在");
        }
        // 不返回token明文
        token.setToken("***");
        return ResultUtils.success(token);
    }

    /**
     * 启用Token
     */
    @PutMapping("/{id}/enable")
    public BaseResponse enableToken(@PathVariable Long id) {
        manageApiTokenBusinessService.updateTokenStatus(id, ManageApiToken.Status.ENABLED);
        return ResultUtils.success();
    }

    /**
     * 禁用Token
     */
    @PutMapping("/{id}/disable")
    public BaseResponse disableToken(@PathVariable Long id) {
        manageApiTokenBusinessService.updateTokenStatus(id, ManageApiToken.Status.DISABLED);
        return ResultUtils.success();
    }

    /**
     * 删除Token
     */
    @DeleteMapping("/{id}")
    public BaseResponse deleteToken(@PathVariable Long id) {
        manageApiTokenBusinessService.deleteToken(id);
        return ResultUtils.success();
    }
}
