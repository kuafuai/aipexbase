package com.kuafuai.manage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.manage.service.ApiManageBusinessService;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.service.ApiMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/market")
public class ApiMarketController {

    private final ApiMarketService apiMarketService;
    private final ApiManageBusinessService apiManageBusinessService;

    @PostMapping("/page")
    public BaseResponse page(@RequestBody ApiMarketVo marketVo) {
        LambdaQueryWrapper<ApiMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ApiMarket::getCreatedAt);
        Page<ApiMarket> page = new Page<>(marketVo.getCurrent(), marketVo.getPageSize());
        return ResultUtils.success(apiMarketService.page(page, queryWrapper));
    }

    @PostMapping("/add")
    public BaseResponse add(@RequestBody ApiMarketVo marketVo) {
        return ResultUtils.success(apiManageBusinessService.createApiMarket(marketVo));
    }

    @PostMapping("/update")
    public BaseResponse update(@RequestBody ApiMarketVo marketVo) {
        return ResultUtils.success(apiManageBusinessService.updateApiMarket(marketVo));
    }

    @GetMapping("/{id}")
    public BaseResponse detail(@PathVariable(value = "id") Integer apiMarketId) {
        ApiMarket apiMarket = apiMarketService.getById(apiMarketId);
        apiMarket.setOwner(Objects.equals(apiMarket.getProviderId(), SecurityUtils.getUserId().intValue()));
        ApiPricing pricing = apiManageBusinessService.getByMarketId(apiMarketId);
        if (pricing != null) {
            apiMarket.setPricingModel(pricing.getPricingModel());
            apiMarket.setUnitPrice(pricing.getUnitPrice());
        } else {
            apiMarket.setPricingModel(0);
            apiMarket.setUnitPrice(0D);
        }
        return ResultUtils.success(apiMarket);
    }

}
