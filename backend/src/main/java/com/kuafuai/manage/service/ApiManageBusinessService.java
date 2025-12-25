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

import java.util.HashMap;
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
        this.restTemplate.setUriTemplateHandler(new org.springframework.web.util.DefaultUriBuilderFactory("http://example.com") {{
            setEncodingMode(org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode.NONE);
        }});
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

    public ApiPricing getByMarketId(Integer marketId) {
        return apiPricingService.lambdaQuery().eq(ApiPricing::getMarketId, marketId).one();
    }

    public ApiTestResultVo testApi(ApiMarketVo marketVo) {
        try {
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

            // 前端已经处理了URL中的模板变量，直接使用传入的URL
            String processedUrl = marketVo.getUrl();

            // 创建请求实体
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求 - 使用处理后的URL，RestTemplate已配置为不处理模板变量
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
            result.setSuccess(response.getStatusCode().is2xxSuccessful());

            return result;
        } catch (Exception e) {
            // 处理异常情况
            ApiTestResultVo result = new ApiTestResultVo();
            result.setStatusCode(0);
            result.setMessage("请求失败: " + e.getMessage());
            result.setResponseBody("");
            result.setSuccess(false);
            
            log.error("API测试请求失败", e);
            return result;
        }
    }
    
    private String replaceTemplateVars(String str, ApiMarketVo marketVo) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        // 解析varRow字段获取变量映射
        Map<String, String> varMapping = parseVarRow(marketVo.getVarRow());
        
        // 替换变量格式，支持 {{var}}、{var}、${var} 和 $var 格式
        // 使用正则表达式匹配模板变量，不使用单词边界以确保URL参数中的变量也能被匹配
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}|\\{\\{([^}]+)\\}\\}|\\{([^}]+)\\}|\\$([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(str);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            // 获取匹配的变量名 (支持四种格式: ${var}, {{var}}, {var}, $var)
            String varName = matcher.group(1); // ${var} 格式
            if (varName == null) {
                varName = matcher.group(2); // {{var}} 格式
            }
            if (varName == null) {
                varName = matcher.group(3); // {var} 格式
            }
            if (varName == null) {
                varName = matcher.group(4); // $var 格式
            }
            
            if (varName == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            varName = varName.trim();
            
            // 防止路径遍历或其他安全问题，只允许字母数字下划线
            if (!varName.matches("^[a-zA-Z0-9_]+$")) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            
            // 优先使用varRow中的变量映射值
            String replacement = varMapping.get(varName);
            
            // 如果varRow中没有该变量，则检查其他特殊变量
            if (replacement == null) {
                if ("token".equals(varName) && marketVo.getToken() != null) {
                    replacement = marketVo.getToken();
                } else {
                    // 变量未找到，保留原样
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                    continue;
                }
            }
            
            // 需要对replacement进行转义，防止特殊字符被误解释
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
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
            
            // 将Object类型的值转换为String
            for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                if (entry.getValue() != null) {
                    varMapping.put(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (Exception e) {
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