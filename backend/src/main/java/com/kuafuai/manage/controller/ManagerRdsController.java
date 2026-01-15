package com.kuafuai.manage.controller;


import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.RdsManager;
import com.kuafuai.manage.entity.dto.RdsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/app")
@ConditionalOnProperty(name = "app.db.enable", havingValue = "true")
public class ManagerRdsController {

    @Resource
    private RdsManager rdsManager;


    @GetMapping("/rds")
    public BaseResponse listRds() {
        return ResultUtils.success(rdsManager.getRdsList());
    }

    @PostMapping("/rds")
    public BaseResponse rdsAdd(@RequestBody RdsDTO rdsDTO) {
        rdsManager.add(rdsDTO);

        return ResultUtils.success();
    }

    @GetMapping("/rds/app")
    public BaseResponse rdsGetByAppId(@RequestParam("app_id") String appId) {
        return ResultUtils.success(DatabaseRouterAspect.getRdsKeyByAppId(appId));
    }
}
