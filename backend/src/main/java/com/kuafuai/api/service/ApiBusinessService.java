package com.kuafuai.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.auth.AuthHandlerFactory;
import com.kuafuai.api.client.ApiClient;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.api.spec.BillingResult;
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
        if (!StringUtils.equalsIgnoreCase(apiMarket.getUrl(), setting.getUrl())) {
            setting.setUrl(apiMarket.getUrl());
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

        // 准备API配置
        ApiMarket apiMarket = getApiMarket(setting.getMarketId());
        mergeMarketSetting(apiMarket, setting);

        Integer billingModel = pricing.getPricingModel();
        BigDecimal unitPrice = BigDecimal.valueOf(pricing.getUnitPrice());

        // 2. 预检查余额
        // 按次计费：检查余额是否足够支付单次调用费用
        if (isPerCallBilling(billingModel) && !hasSufficientBalance(appId, unitPrice)) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }
        // 按token计费：检查余额是否大于0（因为无法提前知道确切的token消耗量）
        if (isPerTokenBilling(billingModel) && !hasSufficientBalance(appId, BigDecimal.ZERO)) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 3. 调用API
        String response = callApiSafely(setting, params, apiMarket, appId);

        // 4. 处理计费
        processBilling(appId, setting, billingModel, unitPrice, response);

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
        if (StringUtils.isEmpty(response)) {
            log.warn("响应为空,无法提取token数量");
            return null;
        }

        // 优先使用自定义路径，如果没有则使用默认路径
        String[] extractPaths = StringUtils.isNotEmpty(tokenPath)
                ? new String[]{tokenPath}
                : new String[]{"$.metadata.usage.total_tokens", "$.usage.total_tokens"};

        return extractTokenFromPaths(response, extractPaths);
    }

    private Integer extractTokenFromPaths(String response, String[] paths) {
        for (String path : paths) {
            Integer tokenCount = extractTokenByPath(response, path);
            if (tokenCount != null) {
                log.info("从路径 {} 成功提取token数量: {}", path, tokenCount);
                return tokenCount;
            }
        }
        return null;
    }

    private Integer extractTokenByPath(String response, String path) {
        try {
            Object tokenObj = JsonPath.read(response, path);
            return convertToInteger(tokenObj);
        } catch (Exception e) {
            log.debug("从路径 {} 提取token数量失败: {}", path, e.getMessage());
            return null;
        }
    }

    private Integer convertToInteger(Object tokenObj) {
        if (tokenObj instanceof Number) {
            return ((Number) tokenObj).intValue();
        } else if (tokenObj instanceof String) {
            try {
                return Integer.parseInt((String) tokenObj);
            } catch (NumberFormatException e) {
                log.warn("token数量字符串格式错误: {}", tokenObj);
                return null;
            }
        }
        log.warn("提取的token数量类型不支持: {}, 类型: {}", tokenObj, tokenObj.getClass().getSimpleName());
        return null;
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


    private String callApiSafely(DynamicApiSetting setting, Map<String, Object> params,
                                 ApiMarket apiMarket, String appId) {
        try {
            return callHttpApi(setting, params, apiMarket);
        } catch (Exception e) {
            log.error("API调用失败,不扣费, appId: {}, apiKey: {}, error: {}", appId, setting.getKeyName(), e.getMessage());
            throw e;
        }
    }

    private void processBilling(String appId, DynamicApiSetting setting, Integer billingModel,
                                BigDecimal unitPrice, String response) {

        // 计算使用量和费用
        BillingResult billingResult = calculateBillingResult(billingModel, unitPrice, response);

        // 执行扣费
        Long userId = getUserIdByAppId(appId);
        boolean deductSuccess = userBalanceService.deductBalance(userId, billingResult.getTotalAmount());
        // 不是免费,并扣费是否成功
        if (!isFreeBilling(billingModel) && !deductSuccess) {
            throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 记录日志
        recordBillingLog(appId, setting, billingModel, billingResult);
    }

    private void recordBillingLog(String appId, DynamicApiSetting setting, Integer billingModel, BillingResult result) {
        // 获取API市场信息以检查isBilling字段
        ApiMarket apiMarket = getApiMarket(setting.getMarketId());
        
        // 如果apiMarke不为null并且isBilling字段为0，则记录计费日志
        if (apiMarket != null && apiMarket.getIsBilling().equals(0)) {
            apiBillingService.recordBilling(appId, setting.getMarketId(), setting.getId(), billingModel, result.getQuantity(), result.getTotalAmount());
            log.info("API调用计费成功, appId: {}, apiKey: {}, billingModel: {}, quantity: {}, totalAmount: {}", appId, setting.getKeyName(), billingModel, result.getQuantity(), result.getTotalAmount());
        } else {
            log.info("API调用计费已跳过(根据isBilling设置)，appId: {}, apiKey: {}, billingModel: {}, quantity: {}, totalAmount: {}", appId, setting.getKeyName(), billingModel, result.getQuantity(), result.getTotalAmount());
        }
    }

    /**
     * 计费-算用量/价格
     */
    private BillingResult calculateBillingResult(Integer billingModel, BigDecimal unitPrice, String response) {
        Integer quantity = extractQuantity(billingModel, response);
        BigDecimal totalAmount = apiBillingService.calculateAmount(billingModel, quantity, unitPrice);

        return BillingResult.builder()
                .quantity(quantity)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 计费-用量
     */
    private Integer extractQuantity(Integer billingModel, String response) {
        if (isPerCallBilling(billingModel) || isFreeBilling(billingModel)) {
            return 1;
        } else if (isPerTokenBilling(billingModel)) {
            return extractTokenCount(response, null);
        }
        return 0;
    }

    /**
     * 按次计费
     */
    private boolean isPerCallBilling(Integer billingModel) {
        return billingModel != null && billingModel == 1;
    }

    /**
     * 按token计费
     */
    private boolean isPerTokenBilling(Integer billingModel) {
        return billingModel != null && billingModel == 2;
    }

    /**
     * 免费
     */
    private boolean isFreeBilling(Integer billingModel) {
        return billingModel == null || billingModel == 0;
    }

    /**
     * 账户余额是否够用
     */
    private boolean hasSufficientBalance(String appId, BigDecimal amount) {
        return checkUserBalance(appId, amount);
    }
}
