package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.Users;
import com.kuafuai.system.mapper.UsersMapper;
import com.kuafuai.system.service.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {
    
    @Override
    public Users getByCodeFlyingUserId(String codeFlyingUserId) {
        log.info("开始查询用户信息，codeFlyingUserId: {}", codeFlyingUserId);
        LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Users::getCodeFlyingUserId, codeFlyingUserId);
        Users user = getOne(queryWrapper);
        log.info("用户信息查询完成，codeFlyingUserId: {}，查询结果: {}", codeFlyingUserId, user);
        return user;
    }
}