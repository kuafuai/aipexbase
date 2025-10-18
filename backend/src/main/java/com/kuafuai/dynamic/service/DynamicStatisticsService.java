package com.kuafuai.dynamic.service;


import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DynamicStatisticsService {

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;

    public BaseResponse count(String database,
                              String table,
                              Map<String, Object> conditions) {

        return ResultUtils.success(dynamicInterfaceService.count(database, table, conditions));
    }
}
