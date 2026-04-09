package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ManageApiToken;
import com.kuafuai.system.mapper.ManageApiTokenMapper;
import com.kuafuai.system.service.ManageApiTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 管理API Token服务实现
 */
@Slf4j
@Service
public class ManageApiTokenServiceImpl extends ServiceImpl<ManageApiTokenMapper, ManageApiToken> implements ManageApiTokenService {

    @Resource
    private ManageApiTokenMapper manageApiTokenMapper;

    @Override
    public ManageApiToken getByToken(String token) {
        LambdaQueryWrapper<ManageApiToken> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ManageApiToken::getToken, token);
        return getOne(queryWrapper);
    }

    @Override
    public void updateLastUsedTime(Long id) {
        manageApiTokenMapper.updateLastUsedTime(id);
    }
}
