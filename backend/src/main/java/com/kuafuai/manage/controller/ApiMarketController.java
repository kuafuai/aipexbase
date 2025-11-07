package com.kuafuai.manage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.service.ApiMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/market")
public class ApiMarketController {

    private final ApiMarketService apiMarketService;

    @PostMapping("/page")
    public BaseResponse page(@RequestBody ApiMarketVo marketVo) {
        LambdaQueryWrapper<ApiMarket> queryWrapper = new LambdaQueryWrapper<>();

        Page<ApiMarket> page = new Page<>(marketVo.getCurrent(), marketVo.getPageSize());
        return ResultUtils.success(apiMarketService.page(page, queryWrapper));
    }
}
