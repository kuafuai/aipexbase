package com.kuafuai.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.config.VersionInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @Autowired
    private VersionInfoHolder versionInfoHolder;

    @GetMapping("/admin/version")
    public BaseResponse version() {
        return ResultUtils.success(versionInfoHolder);
    }
}
