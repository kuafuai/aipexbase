package com.kuafuai.login.service.impl;

import com.kuafuai.common.db.DynamicDataBaseServiceImpl;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.mapper.LoginMapper;
import com.kuafuai.login.service.LoginService;
import org.springframework.stereotype.Service;

/**
* @author www.macpe.cn
* @description 针对表【login(登录表)】的数据库操作Service实现
* @createDate 2025-07-16 20:47:41
*/
@Service
public class LoginServiceImpl extends DynamicDataBaseServiceImpl<LoginMapper, Login>
    implements LoginService{

}




