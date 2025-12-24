package com.kuafuai.manage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.manage.entity.vo.ApiDocumentParsedVo;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.manage.service.ApiManageBusinessService;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.service.ApiMarketService;
import com.kuafuai.config.ApiDocumentParserConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/market")
public class ApiMarketController {

    private final ApiMarketService apiMarketService;
    private final ApiManageBusinessService apiManageBusinessService;
    private final ApiDocumentParserConfig apiDocumentParserConfig;
    private final ObjectMapper objectMapper;
    @Autowired
    @Qualifier("apiDocumentParserRestTemplate")
    private RestTemplate restTemplate;

    @PostMapping("/page")
    public BaseResponse page(@RequestBody ApiMarketVo marketVo) {
        LambdaQueryWrapper<ApiMarket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ApiMarket::getCreatedAt);
        Page<ApiMarket> page = new Page<>(marketVo.getCurrent(), marketVo.getPageSize());
        return ResultUtils.success(apiMarketService.page(page, queryWrapper));
    }

    @GetMapping("/list")
    public BaseResponse list() {
        return ResultUtils.success(apiMarketService.lambdaQuery().orderByDesc(ApiMarket::getCreatedAt).list());
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
        apiMarket.setToken("********");
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

    /**
     * 通过粘贴API文档内容进行解析，并填充到API市场
     * @return 解析后的API信息
     */
    @PostMapping("/parse-document-content")
    public BaseResponse parseDocumentContent(@RequestBody Map<String, Object> requestBody) {
        try {
            String documentContent = null;
            Object contentObj = requestBody.get("documentContent");
            if (contentObj instanceof String) {
                documentContent = (String) contentObj;
            } else if (contentObj != null) {
                // 如果不是字符串，尝试转换为字符串
                documentContent = contentObj.toString();
            }
            
            if (documentContent == null || documentContent.trim().isEmpty()) {
                return ResultUtils.error("文档内容不能为空");
            }
            
            // 准备请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 添加API密钥
            headers.set("Authorization", "Bearer " + apiDocumentParserConfig.getApiKey());
            
            // 构造发送给解析服务的数据
            Map<String, Object> payload = new HashMap<>();
            Map<String, String> inputs = new HashMap<>();
            //inputs.put("document", documentContent);
            payload.put("inputs", inputs);
            // 添加query参数，这里设置一个默认的查询语句
            payload.put("query", documentContent);
            payload.put("response_mode", "blocking");
            // 添加用户信息 - 根据内存知识，使用sys.前缀格式
            payload.put("user", SecurityUtils.getUserId().toString());
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // 发送请求到远程服务
            ResponseEntity<String> response = restTemplate.postForEntity(apiDocumentParserConfig.getUrl(), requestEntity, String.class);
            
            // 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                // 解析返回的JSON数据并转换为ApiMarketVo对象
                String responseBody = response.getBody();
                log.info("Received response from document parser: {}", responseBody);
                
                // 首先尝试解析包装格式的响应
                ApiDocumentParsedVo parsedVo = null;
                try {
                    // 解析外层响应结构
                    Map<String, Object> wrapperResponse = objectMapper.readValue(responseBody, Map.class);
                    
                    // 检查是否存在answer字段（根据用户提供的示例数据结构）
                    if (wrapperResponse.containsKey("answer")) {
                        String answerContent = (String) wrapperResponse.get("answer");
                        // 解析answer字段中的实际内容
                        parsedVo = objectMapper.readValue(answerContent, ApiDocumentParsedVo.class);
                    } else if (wrapperResponse.containsKey("text")) {
                        String textContent = (String) wrapperResponse.get("text");
                        // 解析text字段中的实际内容
                        parsedVo = objectMapper.readValue(textContent, ApiDocumentParsedVo.class);
                    } else {
                        // 如果没有answer或text字段，尝试直接解析
                        parsedVo = objectMapper.readValue(responseBody, ApiDocumentParsedVo.class);
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse response, trying alternative parsing approach", e);
                    // 如果常规解析失败，尝试其他方式
                    try {
                        // 直接将响应体解析为ApiDocumentParsedVo
                        parsedVo = objectMapper.readValue(responseBody, ApiDocumentParsedVo.class);
                    } catch (Exception innerException) {
                        log.error("Failed to parse response even with alternative approach", innerException);
                        return ResultUtils.error("文档解析响应格式不符合预期: " + innerException.getMessage());
                    }
                }
                
                // 转换为ApiMarketVo对象
                ApiMarketVo apiMarketVo = convertToApiMarketVo(parsedVo);
                
                Map<String, Object> result = new HashMap<>();
                result.put("parsedData", parsedVo);
                result.put("apiMarketData", apiMarketVo);
                result.put("message", "文档解析成功");
                return ResultUtils.success(result);
            } else {
                return ResultUtils.error("文档解析失败，状态码: " + response.getStatusCode());
            }
        } catch (ResourceAccessException e) {
            log.error("解析API文档时网络连接错误", e);
            return ResultUtils.error("文档解析服务连接超时，请稍后重试或联系管理员");
        } catch (Exception e) {
            log.error("解析API文档时发生错误", e);
            return ResultUtils.error("文档解析过程中发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 将解析后的数据转换为ApiMarketVo对象
     * @param parsedVo 解析后的数据
     * @return ApiMarketVo对象
     */
    private ApiMarketVo convertToApiMarketVo(ApiDocumentParsedVo parsedVo) {
        ApiMarketVo apiMarketVo = new ApiMarketVo();
        
        // 基本信息转换
        if (parsedVo.getBasic_info() != null) {
            apiMarketVo.setName(parsedVo.getBasic_info().getApi_name());
            apiMarketVo.setDescription(parsedVo.getBasic_info().getApi_description());
            apiMarketVo.setCategory(parsedVo.getBasic_info().getCategory());
            apiMarketVo.setStatus(parsedVo.getBasic_info().getEnabled() != null && parsedVo.getBasic_info().getEnabled() ? 1 : 0);
        }
        
        // API配置转换
        if (parsedVo.getApi_config() != null) {
            apiMarketVo.setUrl(parsedVo.getApi_config().getRequest_url());
            
            // 协议转换 - 只有HTTP和WSS
            if (parsedVo.getApi_config().getProtocol() != null) {
                switch (parsedVo.getApi_config().getProtocol().toUpperCase()) {
                    case "HTTP":
                        apiMarketVo.setProtocol(1);
                        break;
                    case "WSS":
                        apiMarketVo.setProtocol(2);
                        break;
                    default:
                        apiMarketVo.setProtocol(0); // 其他协议设为默认值
                        break;
                }
            }
            
            // 方法转换 - 只有GET和POST
            if (parsedVo.getApi_config().getMethod() != null) {
                switch (parsedVo.getApi_config().getMethod().toUpperCase()) {
                    case "GET":
                        apiMarketVo.setMethod(0);
                        break;
                    case "POST":
                        apiMarketVo.setMethod(1);
                        break;
                    default:
                        apiMarketVo.setMethod(2); // 其他方法设为默认值
                        break;
                }
            }
            
            // 认证配置转换
            if (parsedVo.getApi_config().getAuth_config() != null) {
                apiMarketVo.setAuthType(parsedVo.getApi_config().getAuth_config().getAuth_type());
                apiMarketVo.setToken(parsedVo.getApi_config().getAuth_config().getToken());
            }
            
            // 请求体转换
            if (parsedVo.getApi_config().getRequest_body() != null) {
                String bodyType = parsedVo.getApi_config().getRequest_body().getBody_type();
                if ("json".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(0); // json
                } else if ("form".equalsIgnoreCase(bodyType) || "form-data".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(1); // form-data
                } else if ("urlencoded".equalsIgnoreCase(bodyType) || "x-www-form-urlencoded".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(2); // url-encoded
                } else if ("xml".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(3); // xml
                } else if ("text".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(4); // text
                } else {
                    apiMarketVo.setBodyType(0); // 默认json
                }
                apiMarketVo.setBodyTemplate(parsedVo.getApi_config().getRequest_body().getBody_template());
            }
        }
        
        // 计费信息转换
        if (parsedVo.getBilling_info() != null) {
            String pricingModel = parsedVo.getBilling_info().getPricing_model();
            Boolean isBilling = parsedVo.getBilling_info().getIs_billing();
            
            // 根据pricing_model设置计费模型
            if ("免费".equals(pricingModel) || "free".equalsIgnoreCase(pricingModel)) {
                apiMarketVo.setPricingModel(0); // 免费
                apiMarketVo.setUnitPrice(0.0);
            } else if ("按次计费".equals(pricingModel) || "per_call".equalsIgnoreCase(pricingModel)) {
                apiMarketVo.setPricingModel(1); // 按次计费
            } else if ("按token计费".equals(pricingModel) || "per_token".equalsIgnoreCase(pricingModel)) {
                apiMarketVo.setPricingModel(2); // 按token计费
            }
            
            // 设置是否计费
            apiMarketVo.setIsBilling(isBilling != null && isBilling ? 1 : 0);
        } else {
            // 默认计费模式设为免费
            apiMarketVo.setPricingModel(1); // 免费
            apiMarketVo.setUnitPrice(0.0);
            apiMarketVo.setIsBilling(0); // 不计费
        }
        
        // 响应配置转换
        if (parsedVo.getResponse_config() != null) {
            apiMarketVo.setDataPath(parsedVo.getResponse_config().getData_path());
            
            // 将JsonNode类型的data_row转换为格式化的JSON字符串
            if (parsedVo.getResponse_config().getData_row() != null) {
                try {
                    apiMarketVo.setDataRow(objectMapper.writeValueAsString(parsedVo.getResponse_config().getData_row()));
                } catch (JsonProcessingException e) {
                    log.error("转换data_row为JSON字符串时发生错误", e);
                    // 如果转换失败，回退到默认字符串表示
                    apiMarketVo.setDataRow(parsedVo.getResponse_config().getData_row().toString());
                }
            }
            
            // 数据类型转换
            if (parsedVo.getResponse_config().getData_type() != null) {
                switch (parsedVo.getResponse_config().getData_type().toLowerCase()) {
                    case "text":
                        apiMarketVo.setDataType(0); // text
                        break;
                    case "json":
                        apiMarketVo.setDataType(1); // object
                        break;
                    case "array":
                        apiMarketVo.setDataType(2); // array
                        break;
                    default:
                        apiMarketVo.setDataType(0); // 默认text
                        break;
                }
            }
        }
        
        // 变量配置转换为头部信息和URL参数 (简化处理)
        StringBuilder headersBuilder = new StringBuilder();
        StringBuilder urlParamsBuilder = new StringBuilder();
        if (parsedVo.getVariables_config() != null && !parsedVo.getVariables_config().isEmpty()) {
            boolean isFirstParam = true;
            for (ApiDocumentParsedVo.VariableConfig varConfig : parsedVo.getVariables_config()) {
                if ("header".equalsIgnoreCase(varConfig.getLocation())) {
                    headersBuilder.append(varConfig.getName()).append(": ").append(varConfig.getDefault_value()).append("\n");
                } else if ("url".equalsIgnoreCase(varConfig.getLocation())) {
                    // 处理URL参数
                    if (isFirstParam) {
                        urlParamsBuilder.append(varConfig.getName()).append("=${").append(varConfig.getName()).append("}");
                        isFirstParam = false;
                    } else {
                        urlParamsBuilder.append("&").append(varConfig.getName()).append("=${").append(varConfig.getName()).append("}");
                    }
                }
            }
            
            // 如果URL中没有参数占位符但有URL变量，则将它们附加到URL
            if (parsedVo.getApi_config() != null && !urlParamsBuilder.toString().isEmpty()) {
                String currentUrl = apiMarketVo.getUrl();
                if (currentUrl != null && !currentUrl.contains("${")) {
                    // 在URL后面添加参数
                    apiMarketVo.setUrl(currentUrl + (currentUrl.contains("?") ? "&" : "?") + urlParamsBuilder.toString());
                }
            }
        }
        apiMarketVo.setHeaders(headersBuilder.toString());
        
        return apiMarketVo;
    }
}