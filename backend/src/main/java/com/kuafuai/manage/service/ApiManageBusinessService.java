package com.kuafuai.manage.service;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.service.ApiMarketService;
import com.kuafuai.system.service.ApiPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ApiManageBusinessService {

    @Autowired
    private ApiMarketService apiMarketService;
    @Autowired
    private ApiPricingService apiPricingService;


    @Transactional
    public boolean createApiMarket(ApiMarketVo marketVo) {
        Long owner = SecurityUtils.getUserId();

        ApiMarket market = ApiMarket.builder()
                .providerId(owner.intValue())
                .name(marketVo.getName())
                .description(marketVo.getDescription())
                .category(marketVo.getCategory())
                .url(marketVo.getUrl())
                .method(marketVo.getMethod())
                .protocol(marketVo.getProtocol())
                .authType(marketVo.getAuthType())
                .authConfig(marketVo.getAuthConfig())
                .token(marketVo.getToken())
                .bodyType(marketVo.getBodyType())
                .bodyTemplate(marketVo.getBodyTemplate())
                .headers(marketVo.getHeaders())
                .dataPath(marketVo.getDataPath())
                .dataType(marketVo.getDataType())
                .dataRow(marketVo.getDataRow())
                .varRow(marketVo.getVarRow())
                .status(marketVo.getStatus())
                .build();
        apiMarketService.save(market);
        Integer marketId = market.getId();

        ApiPricing apiPricing = ApiPricing.builder()
                .marketId(marketId)
                .pricingModel(marketVo.getPricingModel())
                .unitPrice(marketVo.getUnitPrice())
                .build();

        return apiPricingService.save(apiPricing);
    }

    public ApiPricing getByMarketId(Integer marketId) {
        return apiPricingService.lambdaQuery().eq(ApiPricing::getMarketId, marketId).one();
    }

    @Transactional
    public boolean updateApiMarket(ApiMarketVo marketVo) {
        ApiMarket market = apiMarketService.getById(marketVo.getId());
        if (!Objects.equals(market.getProviderId(), SecurityUtils.getUserId().intValue())) {
            throw new BusinessException("error.code.no_auth");
        }

        ApiMarket updateMarket = ApiMarket.builder()
                .id(market.getId())
                .providerId(market.getProviderId())
                .name(marketVo.getName())
                .description(marketVo.getDescription())
                .category(marketVo.getCategory())
                .url(marketVo.getUrl())
                .method(marketVo.getMethod())
                .protocol(marketVo.getProtocol())
                .authType(marketVo.getAuthType())
                .authConfig(marketVo.getAuthConfig())
                .token(marketVo.getToken())
                .bodyType(marketVo.getBodyType())
                .bodyTemplate(marketVo.getBodyTemplate())
                .headers(marketVo.getHeaders())
                .dataPath(marketVo.getDataPath())
                .dataType(marketVo.getDataType())
                .dataRow(marketVo.getDataRow())
                .varRow(marketVo.getVarRow())
                .status(marketVo.getStatus())
                .build();

        apiMarketService.updateById(updateMarket);

        // 先删除，后添加
        apiPricingService.lambdaUpdate()
                .eq(ApiPricing::getMarketId, market.getId()).remove();

        ApiPricing apiPricing = ApiPricing.builder()
                .marketId(market.getId())
                .pricingModel(marketVo.getPricingModel())
                .unitPrice(marketVo.getUnitPrice())
                .build();

        return apiPricingService.save(apiPricing);
    }
}
