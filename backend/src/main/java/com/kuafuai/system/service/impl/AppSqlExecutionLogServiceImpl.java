package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppSqlExecutionLog;
import com.kuafuai.system.mapper.AppSqlExecutionLogMapper;
import com.kuafuai.system.service.AppSqlExecutionLogService;
import org.springframework.stereotype.Service;

@Service
public class AppSqlExecutionLogServiceImpl extends ServiceImpl<AppSqlExecutionLogMapper, AppSqlExecutionLog>
        implements AppSqlExecutionLogService {
}
