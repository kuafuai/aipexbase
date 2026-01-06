package com.kuafuai.manage.service;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.manage.entity.vo.ApiTestResultVo;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.ApiPricing;
import com.kuafuai.system.service.ApiMarketService;
import com.kuafuai.system.service.ApiPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiManageBusinessService {

    @Autowired
    private ApiMarketService apiMarketService;
    @Autowired
    private ApiPricingService apiPricingService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final RestTemplate restTemplate;
    
    public ApiManageBusinessService() {
        this.restTemplate = new RestTemplate();
        // 设置连接超时和读取超时
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(60000); // 60秒连接超时
        factory.setReadTimeout(120000);    // 120秒读取超时
        this.restTemplate.setRequestFactory(factory);
        // 禁用URI模板变量替换以避免Spring处理URL中的${}格式变量
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        // 设置不解析模板变量
        uriFactory.setParsePath(false);
        this.restTemplate.setUriTemplateHandler(uriFactory);
    }


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
                .isBilling(marketVo.getIsBilling()) // 添加isBilling字段
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

    @Transactional
    public Map<String, Object> createApiMarketWithTest(ApiMarketVo marketVo) {
        // 首先进行API测试
        log.info("开始API测试，URL: {}", marketVo.getUrl());
        ApiTestResultVo testResult = testApi(marketVo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("testResult", testResult);
        
        if (testResult.getSuccess()) {
            log.info("API测试成功，开始保存API");
            // 测试成功，保存API
            boolean saveSuccess = createApiMarket(marketVo);
            result.put("saved", saveSuccess);
            result.put("message", saveSuccess ? "API测试成功，已自动保存" : "API测试成功，但保存失败");
            if (saveSuccess) {
                log.info("API已成功保存");
            } else {
                log.warn("API测试成功，但保存失败");
            }
        } else {
            log.warn("API测试失败，不保存，状态码: {}, 消息: {}", testResult.getStatusCode(), testResult.getMessage());
            // 测试失败，不保存
            result.put("saved", false);
            result.put("message", "API测试失败，未保存");
            
            // 分析可能缺少的内容
            List<String> missingFields = analyzeMissingFields(marketVo, testResult);
            result.put("missingFields", missingFields);
            log.warn("可能缺少的字段: {}", missingFields);
            
            // 将marketVo也返回，以便前端可以跳转到添加界面并填充数据
            result.put("apiMarketData", marketVo);
        }
        
        return result;
    }
    
    /**
     * 分析API测试失败时可能缺少的内容
     * @param marketVo API市场对象
     * @param testResult 测试结果
     * @return 缺少的字段列表
     */
    public List<String> analyzeMissingFields(ApiMarketVo marketVo, ApiTestResultVo testResult) {
        List<String> missingFields = new ArrayList<>();
        
        // 检查URL是否为空
        if (marketVo.getUrl() == null || marketVo.getUrl().trim().isEmpty()) {
            missingFields.add("API请求URL");
        }
        
        // 检查认证信息是否完整
        if (marketVo.getAuthType() != null && !marketVo.getAuthType().equalsIgnoreCase("None")) {
            if (marketVo.getToken() == null || marketVo.getToken().trim().isEmpty()) {
                missingFields.add("认证Token");
            }
        }
        
        // 检查请求方法
        if (marketVo.getMethod() == null) {
            missingFields.add("请求方法");
        }
        
        // 优先检查testResult中的message字段（可能包含AI分析提取的error_message）
        if (testResult.getMessage() != null && !testResult.getMessage().trim().isEmpty()) {
            // 检查是否包含具体的错误信息
            String message = testResult.getMessage().toLowerCase();
            if (message.contains("error_message") || message.contains("非app_id请求方式") || 
                message.contains("错误") || message.contains("失败") || message.contains("invalid") ||
                message.contains("app_id") || message.contains("app_secret")) {
                // 这是具体的错误信息，直接添加
                missingFields.add(testResult.getMessage());
                return missingFields;
            }
        }
        
        // 检查响应体中的具体错误信息
        if (testResult.getResponseBody() != null && !testResult.getResponseBody().trim().isEmpty()) {
            try {
                // 解析响应体以获取更具体的错误信息
                Map<String, Object> responseMap = objectMapper.readValue(testResult.getResponseBody(), Map.class);
                
                // 检查reason字段（如示例中的 "reason":"错误的请求KEY"）
                Object reason = responseMap.get("reason");
                if (reason != null) {
                    missingFields.add(reason.toString());
                    return missingFields; // 直接返回具体的错误信息，不添加其他通用提示
                }
                
                // 检查其他可能的错误信息字段
                Object message = responseMap.get("message");
                if (message != null) {
                    missingFields.add(message.toString());
                    return missingFields;
                }
                
                Object msg = responseMap.get("msg");
                if (msg != null) {
                    missingFields.add(msg.toString());
                    return missingFields;
                }
                
            } catch (Exception e) {
                log.warn("解析响应体JSON失败，无法提取具体错误信息: {}", e.getMessage());
            }
        }
        
        // 根据HTTP状态码分析问题
        if (testResult.getStatusCode() != null) {
            if (testResult.getStatusCode() == 401) {
                missingFields.add("认证信息（状态码401表示未授权）");
            } else if (testResult.getStatusCode() == 403) {
                missingFields.add("访问权限（状态码403表示禁止访问）");
            } else if (testResult.getStatusCode() == 404) {
                missingFields.add("API端点路径（状态码404表示资源不存在）");
            } else if (testResult.getStatusCode() == 422 || testResult.getStatusCode() == 400) {
                missingFields.add("必需的请求参数或请求体（状态码400/422表示请求参数错误）");
            } else if (testResult.getStatusCode() >= 500) {
                missingFields.add("服务器内部问题（状态码5xx表示服务器错误）");
            }
        }
        
        // 如果没有识别出特定问题，添加一般性提示
        if (missingFields.isEmpty()) {
            missingFields.add("有效的API端点、正确的认证信息或合适的请求参数");
        }
        
        return missingFields;
    }

    public ApiPricing getByMarketId(Integer marketId) {
        return apiPricingService.lambdaQuery().eq(ApiPricing::getMarketId, marketId).one();
    }

    public ApiTestResultVo testApi(ApiMarketVo marketVo) {
        try {
            log.debug("Testing API with URL: {}, varRow: {}", marketVo.getUrl(), marketVo.getVarRow());
            
            // 解析headers
            HttpHeaders headers = new HttpHeaders();
            if (marketVo.getHeaders() != null && !marketVo.getHeaders().trim().isEmpty()) {
                String headerStr = marketVo.getHeaders().trim();
                
                // 尝试解析JSON格式的headers
                if (headerStr.startsWith("{")) {
                    try {
                        // 使用Jackson解析JSON格式的headers
                        Map<String, Object> headersMap = objectMapper.readValue(headerStr, Map.class);
                        for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                            if (entry.getValue() != null) {
                                headers.add(entry.getKey(), entry.getValue().toString());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse headers as JSON, trying multi-line format", e);
                        // 如果JSON解析失败，尝试多行格式
                        parseMultiLineHeaders(headers, headerStr);
                    }
                } else {
                    // 解析多行格式的headers: "key1: value1\nkey2: value2\nkey3: value3"
                    parseMultiLineHeaders(headers, headerStr);
                }
            }

            // 根据认证类型处理认证头
            if (marketVo.getAuthType() != null && !marketVo.getAuthType().trim().isEmpty() && marketVo.getToken() != null) {
                if ("Bearer".equalsIgnoreCase(marketVo.getAuthType())) {
                    headers.add("Authorization", "Bearer " + marketVo.getToken());
                }
                // 当AuthType为"None"时不添加任何认证头，只使用用户在headers中填写的内容
            }
            
            // 根据bodyType设置Content-Type
            if (marketVo.getBodyType() != null) {
                switch (marketVo.getBodyType()) {
                    case 0: // json
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "application/json");
                        }
                        break;
                    case 1: // form-data
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "multipart/form-data");
                        }
                        break;
                    case 2: // url-encoded
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "application/x-www-form-urlencoded");
                        }
                        break;
                    case 3: // xml
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "application/xml");
                        }
                        break;
                    case 4: // text
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "text/plain");
                        }
                        break;
                }
            }

            // 准备请求体
            Object requestBody = null;
            if (marketVo.getBodyTemplate() != null && !marketVo.getBodyTemplate().trim().isEmpty()) {
                String bodyTemplate = replaceTemplateVars(marketVo.getBodyTemplate(), marketVo);
                // 根据bodyType处理请求体
                if (marketVo.getBodyType() != null && (marketVo.getBodyType() == 0 || marketVo.getBodyType() == 3)) { // JSON or XML
                    // 尝试解析JSON/XML格式的请求体
                    try {
                        requestBody = bodyTemplate; // 对于JSON和XML，直接使用字符串
                    } catch (Exception e) {
                        // 如果解析失败，使用原始字符串
                        requestBody = bodyTemplate;
                    }
                } else {
                    requestBody = bodyTemplate; // 对于其他类型，使用字符串
                }
            }

            // 确定HTTP方法
            HttpMethod httpMethod = null;
            if (marketVo.getMethod() != null) {
                switch (marketVo.getMethod()) {
                    case 0: // GET
                        httpMethod = HttpMethod.GET;
                        break;
                    case 1: // POST
                        httpMethod = HttpMethod.POST;
                        break;
                    case 2: // PUT
                        httpMethod = HttpMethod.PUT;
                        break;
                    case 3: // DELETE
                        httpMethod = HttpMethod.DELETE;
                        break;
                    case 4: // PATCH
                        httpMethod = HttpMethod.PATCH;
                        break;
                    default:
                        httpMethod = HttpMethod.GET; // 默认GET
                        break;
                }
            } else {
                httpMethod = HttpMethod.GET; // 默认GET
            }
            
            // 如果是GET请求，不能有请求体
            if (httpMethod == HttpMethod.GET) {
                requestBody = null;
            }

            // 处理URL中的模板变量 - 这是关键部分，需要确保URL不包含任何未替换的模板变量
            log.debug("Original URL: {}", marketVo.getUrl());
            String processedUrl = replaceTemplateVars(marketVo.getUrl(), marketVo);
            log.debug("Processed URL: {}", processedUrl);
            
            // 如果URL在变量替换后为空或无效，使用原始URL作为备选
            if (processedUrl == null || processedUrl.trim().isEmpty()) {
                processedUrl = marketVo.getUrl();
            }

            // 确保URL是有效的（不包含任何模板格式）
            // 验证URL中是否还有模板格式
            if (processedUrl != null && (processedUrl.contains("${") || processedUrl.contains("{{") || processedUrl.contains("{"))) {
                // 如果还有模板格式，说明变量替换失败，记录错误
                log.error("URL still contains template variables after processing: " + processedUrl);
                // 返回错误结果
                ApiTestResultVo errorResult = new ApiTestResultVo();
                errorResult.setStatusCode(400);
                errorResult.setMessage("URL contains unresolved template variables: " + processedUrl);
                errorResult.setResponseBody("");
                errorResult.setSuccess(false);
                return errorResult;
            }

            // 创建请求实体
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求 - 使用处理后的URL，RestTemplate已配置为不处理模板变量
            log.debug("Sending request to: {}", processedUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    processedUrl,
                    httpMethod,
                    requestEntity,
                    String.class
            );

            // 构建返回结果
            ApiTestResultVo result = new ApiTestResultVo();
            result.setStatusCode(response.getStatusCodeValue());
            result.setMessage(response.getStatusCode().getReasonPhrase());
            result.setResponseBody(response.getBody());
            
            // 只检查HTTP状态码，不检查响应体中的业务逻辑错误
            boolean httpSuccess = response.getStatusCode().is2xxSuccessful();
            result.setSuccess(httpSuccess);
            
            // 记录API测试结果到控制台
            log.info("API测试结果 - 状态码: {}, HTTP成功: {}, 总体成功: {}, 消息: {}", 
                result.getStatusCode(), httpSuccess, result.getSuccess(), result.getMessage());
            if (result.getResponseBody() != null) {
                log.debug("API测试响应体: {}", result.getResponseBody());
            }

            return result;
        } catch (Exception e) {
            // 处理异常情况
            ApiTestResultVo result = new ApiTestResultVo();
            result.setStatusCode(0);
            result.setMessage("请求失败: " + e.getMessage());
            result.setResponseBody("");
            result.setSuccess(false);
            
            log.error("API测试请求失败", e);
            // 记录失败的测试结果到控制台
            log.info("API测试结果 - 状态码: {}, 成功: {}, 消息: {}", 
                result.getStatusCode(), result.getSuccess(), result.getMessage());
            return result;
        }
    }

    private static final Pattern TEMPLATE_PATTERN =
            Pattern.compile(
                    "\\$\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}" + // ${{var}}
                    "|\\$\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}" +     // ${var}
                    "|\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}"     // {{var}}
            );


    private String replaceTemplateVars(String str, ApiMarketVo marketVo) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        log.debug("Processing string for variable replacement: {}", str);

        // 解析 varRow
        Map<String, String> varMapping = parseVarRow(marketVo.getVarRow());
        log.debug("Variable mapping from varRow: {}", varMapping);

        Matcher matcher = TEMPLATE_PATTERN.matcher(str);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName =
                    matcher.group(1) != null ? matcher.group(1) :
                    matcher.group(2) != null ? matcher.group(2) :
                    matcher.group(3);


            log.debug("Found variable to replace: {}", varName);

            String value = varMapping.get(varName);

            if (value == null) {
                log.warn("No value found for variable '{}', keeping original", varName);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);

        log.debug("Final result after variable replacement: {}", result);
        return result.toString();
    }

    
    /**
     * 解析varRow字段，将其转换为变量映射Map
     * @param varRow JSON格式的变量映射字符串
     * @return 变量名到值的映射
     */
    private Map<String, String> parseVarRow(String varRow) {
        Map<String, String> varMapping = new HashMap<>();
        
        if (varRow == null || varRow.trim().isEmpty()) {
            return varMapping;
        }
        
        try {
            // 尝试解析JSON格式的varRow - 首先尝试作为对象映射
            Map<String, Object> jsonMap = objectMapper.readValue(varRow, Map.class);
            
            // 检查是否为简单的键值对格式（如 {"search": "奥迪A6L"}）
            // 或复杂格式（如 {"search": {"value": "奥迪A6L", "desc": ""}}）
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                Object value = entry.getValue();
                
                if (value instanceof Map) {
                    // 复杂格式：{"search": {"value": "奥迪A6L", "desc": ""}}
                    Map<String, Object> valueMap = (Map<String, Object>) value;
                    Object valueObj = valueMap.get("value");
                    if (valueObj != null) {
                        varMapping.put(entry.getKey(), valueObj.toString());
                    } else {
                        // 如果没有value字段，尝试使用整个对象的字符串表示
                        varMapping.put(entry.getKey(), value.toString());
                    }
                } else {
                    // 简单格式：{"search": "奥迪A6L"}
                    varMapping.put(entry.getKey(), value.toString());
                }
            }
            log.debug("Parsed varRow successfully: {}", varMapping); // 添加调试日志
        } catch (Exception e) {
            log.warn("Failed to parse varRow as JSON object, trying alternative format", e);
            // 如果作为简单对象映射解析失败，尝试作为对象数组解析
            try {
                // 尝试解析为包含name和value字段的对象数组
                java.util.List<Map<String, Object>> jsonArray = objectMapper.readValue(varRow, 
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, Map.class));
                
                // 如果是数组，解析每个元素
                for (Map<String, Object> varObj : jsonArray) {
                    String name = (String) varObj.get("name");
                    String value = (String) varObj.get("value");
                    
                    if (name != null && value != null) {
                        varMapping.put(name, value);
                    }
                }
            } catch (Exception arrayException) {
                log.warn("Failed to parse varRow as JSON object or array, trying alternative format", arrayException);
                
                // 如果JSON解析都失败，尝试其他格式（如key=value对）
                parseVarRowAlternative(varRow, varMapping);
            }
        }
        
        return varMapping;
    }
    
    /**
     * 解析varRow的替代格式
     * @param varRow 变量映射字符串
     * @param varMapping 用于存储解析结果的映射
     */
    private void parseVarRowAlternative(String varRow, Map<String, String> varMapping) {
        // 按行分割
        String[] lines = varRow.split("\\r?\\n|\\r");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // 跳过空行和注释行
            }
            
            // 按等号分割key和value
            int equalsIndex = line.indexOf("=");
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                varMapping.put(key, value);
            }
        }
    }

    /**
     * 解析多行格式的headers
     * @param headers HttpHeaders对象
     * @param headerStr headers字符串
     */
    private void parseMultiLineHeaders(HttpHeaders headers, String headerStr) {
        String[] headerLines = headerStr.split("\n");
        for (String headerLine : headerLines) {
            headerLine = headerLine.trim();
            if (!headerLine.isEmpty()) {
                int colonIndex = headerLine.indexOf(":");
                if (colonIndex > 0) {
                    String key = headerLine.substring(0, colonIndex).trim();
                    String value = headerLine.substring(colonIndex + 1).trim();
                    headers.add(key, value);
                }
            }
        }
    }

    @Transactional
    public boolean updateApiMarket(ApiMarketVo marketVo) {
        ApiMarket market = apiMarketService.getById(marketVo.getId());
        if (!Objects.equals(market.getProviderId(), SecurityUtils.getUserId().intValue())) {
            throw new BusinessException("error.code.no_auth");
        }

        String token = market.getToken();
        if (!StringUtils.contains(marketVo.getToken(), "*")) {
            token = marketVo.getToken();
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
                .token(token)
                .bodyType(marketVo.getBodyType())
                .bodyTemplate(marketVo.getBodyTemplate())
                .headers(marketVo.getHeaders())
                .dataPath(marketVo.getDataPath())
                .dataType(marketVo.getDataType())
                .dataRow(marketVo.getDataRow())
                .varRow(marketVo.getVarRow())
                .status(marketVo.getStatus())
                .isBilling(marketVo.getIsBilling()) // 添加isBilling字段
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