package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.Users;

public interface UsersService extends IService<Users> {
    
    /**
     * 根据codeflying_user_id获取用户
     * @param codeFlyingUserId 码上飞用户ID
     * @return 用户对象
     */
    Users getByCodeFlyingUserId(String codeFlyingUserId);
}