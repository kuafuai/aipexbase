package com.kuafuai.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.auth.AuthHandlerFactory;
import com.kuafuai.api.client.ApiClient;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class ApiBusinessService {


    @Autowired
    private DynamicApiSettingService dynamicApiSettingService;

    @Autowired
    private ApiMarketService apiMarketService;

    @Autowired
    private ApiPricingService apiPricingService;

    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private UserBalanceService userBalanceService;

    @Autowired
    private ApiBillingService apiBillingService;

    @Autowired
    private AuthHandlerFactory authHandlerFactory;

    private final ApiClient apiClient = new ApiClient();

    public String callApi(String appId, String apiKey, Map<String, Object> params) {
        // 查询 api 记录
        DynamicApiSetting setting = getByApiKey(appId, apiKey);
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (StringUtils.equalsIgnoreCase(setting.getProtocol(), "http")) {
            return callHttpApi(setting, params, null);
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    public String callHttpApi(DynamicApiSetting setting, Map<String, Object> params, ApiMarket apiMarket) {
        ApiDefinition apiDefinition = getApiBySetting(setting);

        // 判断鉴权方式
        process_auth_type(setting, apiDefinition, apiMarket, params);

        return apiClient.call(apiDefinition, params);
    }

    public DynamicApiSetting getByApiKey(String appId, String apiKey) {
        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);
        queryWrapper.eq(DynamicApiSetting::getKeyName, apiKey);

        return dynamicApiSettingService.getOne(queryWrapper);
    }

    private ApiDefinition getApiBySetting(DynamicApiSetting setting) {
        return ApiDefinition.builder()
                .name(setting.getKeyName())
                .method(setting.getMethod())
                .url(setting.getUrl())
                .headers(setting.getHeader())
                .bodyType(setting.getBodyType())
                .bodyTemplate(setting.getBodyTemplate())
                .build();
    }

    private void mergeMarketSetting(ApiMarket apiMarket, DynamicApiSetting setting) {
        setting.setToken(apiMarket.getToken());
        setting.setHeader(apiMarket.getHeaders());
        setting.setBodyTemplate(apiMarket.getBodyTemplate());
        if (StringUtils.isNotEmpty(apiMarket.getBodyTemplate())) {
            if (apiMarket.getBodyType() == 1) {
                setting.setBodyType("form");
            } else {
                setting.setBodyType("template");
            }
        } else {
            setting.setBodyType("");
        }
        if (!StringUtils.equalsIgnoreCase(apiMarket.getDataPath(), setting.getDataPath())) {
            setting.setDataPath(apiMarket.getDataPath());
        }
    }

    /**
     * 执行API调用并计费(事务方法)
     *
     * @param appId   应用ID
     * @param setting API配置
     * @param params  请求参数
     * @return API响应结果
     */
    @Transactional(rollbackFor = Exception.class)
    public String callApiWithBilling(String appId, DynamicApiSetting setting, Map<String, Object> params) {
        // 1. 获取定价信息
        ApiPricing pricing = getApiPricing(setting.getMarketId());
        if (pricing == null) {
            log.warn("API未配置定价信息,跳过计费, appId: {}, apiKey: {}", appId, setting.getKeyName());
            return callHttpApi(setting, params, null);
        }

        ApiMarket apiMarket = getApiMarket(setting.getMarketId());
        mergeMarketSetting(apiMarket, setting);

        Integer billingModel = pricing.getPricingModel();
        BigDecimal unitPrice = BigDecimal.valueOf(pricing.getUnitPrice());

        // 2. 预估费用并检查余额
        BigDecimal estimatedAmount;
        if (billingModel == 1) {
            // 按次收费
            estimatedAmount = unitPrice;
        } else if (billingModel == 2) {
            // 按token收费,暂时无法预估,跳过余额检查
            estimatedAmount = BigDecimal.ZERO;
        } else {
            // Free 免费
            estimatedAmount = BigDecimal.ZERO;
            unitPrice = BigDecimal.ZERO;
        }

        // 3. 检查余额(按次收费时检查,按token收费时先调用再扣费)
        if (billingModel != 0 && !checkUserBalance(appId, estimatedAmount)) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 4. 调用API
        String response;
        try {
            response = callHttpApi(setting, params, apiMarket);
        } catch (Exception e) {
            log.error("API调用失败,不扣费, appId: {}, apiKey: {}, error: {}", appId, setting.getKeyName(), e.getMessage());
            throw e;
        }

        // 5. 计算实际费用
        Integer quantity;
        if (billingModel == 1) {
            // 按次收费
            quantity = 1;
        } else if (billingModel == 2) {
            // 按token收费,从响应中提取token数量(TODO: tokenPath需要配置)
            quantity = extractTokenCount(response, null);
            if (quantity == null || quantity <= 0) {
                log.warn("无法提取token数量,跳过计费, appId: {}, apiKey: {}", appId, setting.getKeyName());
                return response;
            }
        } else {
            // Free
            quantity = 1;
        }

        BigDecimal totalAmount = apiBillingService.calculateAmount(billingModel, quantity, unitPrice);

        // 6. 扣费并记录
        Long userId = getUserIdByAppId(appId);
        boolean deductSuccess = userBalanceService.deductBalance(userId, totalAmount);
        if (billingModel != 0 && !deductSuccess) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 7. 记录计费日志
        apiBillingService.recordBilling(appId, setting.getMarketId(), setting.getId(), billingModel, quantity, unitPrice);

        log.info("API调用计费成功, appId: {}, apiKey: {}, billingModel: {}, quantity: {}, totalAmount: {}", appId, setting.getKeyName(), billingModel, quantity, totalAmount);

        return response;
    }

    public ApiMarket getApiMarket(Integer marketId) {
        if (marketId == null) {
            return null;
        }
        return apiMarketService.getById(marketId);
    }

    /**
     * 获取API定价信息
     *
     * @param marketId API市场ID
     * @return 定价信息
     */
    public ApiPricing getApiPricing(Integer marketId) {
        if (marketId == null) {
            return null;
        }
        LambdaQueryWrapper<ApiPricing> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiPricing::getMarketId, marketId);
        return apiPricingService.getOne(queryWrapper);
    }

    /**
     * 通过appId获取userId
     *
     * @param appId 应用ID
     * @return 用户ID
     */
    public Long getUserIdByAppId(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        return appInfo.getOwner();
    }

    /**
     * 检查用户余额是否充足
     *
     * @param appId  应用ID
     * @param amount 需要的金额
     * @return 是否充足
     */
    public boolean checkUserBalance(String appId, BigDecimal amount) {
        Long userId = getUserIdByAppId(appId);
        return userBalanceService.checkBalance(userId, amount);
    }

    /**
     * 从API响应中提取token数量(预留方法)
     *
     * @param response  API响应
     * @param tokenPath token数量的JSONPath路径
     * @return token数量, 解析失败返回null
     */
    public Integer extractTokenCount(String response, String tokenPath) {
        if (StringUtils.isEmpty(response) || StringUtils.isEmpty(tokenPath)) {
            log.warn("响应或token路径为空,无法提取token数量");
            return null;
        }

        try {
            Object tokenObj = JsonPath.read(response, tokenPath);
            if (tokenObj instanceof Number) {
                return ((Number) tokenObj).intValue();
            } else if (tokenObj instanceof String) {
                return Integer.parseInt((String) tokenObj);
            }
            log.warn("提取的token数量类型不支持: {}", tokenObj.getClass());
            return null;
        } catch (Exception e) {
            log.error("提取token数量失败: {}", e.getMessage());
            return null;
        }
    }

    private void process_auth_type(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) {

        String authType;
        if (apiMarket == null) {
            authType = "default";
        } else {
            authType = apiMarket.getAuthType();
            if (StringUtils.equalsAnyIgnoreCase(authType, "None", "Bearer")) {
                authType = "default";
            }
        }
        AuthHandler handler = authHandlerFactory.getHandler(authType);

        try {
            handler.handle(setting, apiDefinition, apiMarket, params);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "认证处理失败: " + e.getMessage());
        }
    }
}
