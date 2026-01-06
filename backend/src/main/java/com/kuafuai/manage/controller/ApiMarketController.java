package com.kuafuai.manage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.manage.entity.vo.ApiDocumentParsedVo;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.manage.entity.vo.ApiTestResultVo;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    @Autowired
    @Qualifier("apiAnalysisRestTemplate")
    private RestTemplate analysisRestTemplate;

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
                        Object answerObj = wrapperResponse.get("answer");
                        String answerContent = null;
                        
                        if (answerObj instanceof String) {
                            answerContent = (String) answerObj;
                            // 检查answerContent是否是JSON格式还是包含API信息的字符串
                            if (answerContent.trim().startsWith("{") || answerContent.trim().startsWith("[")) {
                                // 如果是JSON格式，直接解析
                                parsedVo = objectMapper.readValue(answerContent, ApiDocumentParsedVo.class);
                            } else {
                                // 如果是包含API信息的字符串格式，如 "url,method,headers,body"，需要解析成ApiDocumentParsedVo对象
                                parsedVo = parseApiInfoFromString(answerContent);
                            }
                        } else {
                            // 如果answer不是字符串而是对象，直接解析
                            answerContent = objectMapper.writeValueAsString(answerObj);
                            parsedVo = objectMapper.readValue(answerContent, ApiDocumentParsedVo.class);
                        }
                    } else if (wrapperResponse.containsKey("text")) {
                        String textContent = (String) wrapperResponse.get("text");
                        // 解析text字段中的实际内容
                        parsedVo = objectMapper.readValue(textContent, ApiDocumentParsedVo.class);
                    } else {
                        // 如果没有answer或text字段，尝试直接解析
                        parsedVo = objectMapper.readValue(responseBody, ApiDocumentParsedVo.class);
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse response as JSON, trying alternative parsing approach", e);
                    // 如果常规解析失败，尝试其他方式
                    try {
                        // 检查是否是包含API信息的字符串格式
                        if (responseBody.trim().startsWith("{") || responseBody.trim().startsWith("[")) {
                            // 直接将响应体解析为ApiDocumentParsedVo
                            parsedVo = objectMapper.readValue(responseBody, ApiDocumentParsedVo.class);
                        } else {
                            // 解析包含API信息的字符串格式
                            parsedVo = parseApiInfoFromString(responseBody);
                        }
                    } catch (Exception innerException) {
                        log.error("Failed to parse response even with alternative approach", innerException);
                        return ResultUtils.error("文档解析响应格式不符合预期: " + innerException.getMessage());
                    }
                }
                
                // 转换为ApiMarketVo对象
                ApiMarketVo apiMarketVo = convertToApiMarketVo(parsedVo);
                
                // 现在根据解析的API信息进行测试
                // 使用解析得到的API信息构造测试请求
                ApiTestResultVo testResult = apiManageBusinessService.testApi(apiMarketVo);
                
                // 不在代码中检测响应体的测试结果，而是直接使用外部AI分析接口返回的值来判断
                // 调用AI分析接口来评估测试结果
                testResult = analyzeTestFailureWithAI(testResult, apiMarketVo);
                // AI分析结果将决定最终的成功状态
                
                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("parsedData", parsedVo);
                result.put("apiMarketData", apiMarketVo);
                result.put("testResult", testResult);
                
                if (testResult.getSuccess()) {
                    // 测试成功，自动保存API
                    boolean saveSuccess = apiManageBusinessService.createApiMarket(apiMarketVo);
                    result.put("saved", saveSuccess);
                    result.put("message", saveSuccess ? "API测试成功，已自动保存" : "API测试成功，但保存失败");
                } else {
                    // 测试失败，不保存
                    result.put("saved", false);
                    result.put("message", "API测试失败，未保存");
                    
                    // 分析可能缺少的内容
                    List<String> missingFields = apiManageBusinessService.analyzeMissingFields(apiMarketVo, testResult);
                    result.put("missingFields", missingFields);
                    log.warn("可能缺少的字段: {}", missingFields);
                }
                
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
            
            // 处理请求头
            if (parsedVo.getApi_config().getHeaders() != null) {
                // 如果headers是Map类型（JSON对象），转换为JSON字符串
                if (parsedVo.getApi_config().getHeaders() instanceof Map) {
                    try {
                        apiMarketVo.setHeaders(objectMapper.writeValueAsString(parsedVo.getApi_config().getHeaders()));
                    } catch (Exception e) {
                        log.error("转换headers为JSON字符串时发生错误", e);
                        apiMarketVo.setHeaders(parsedVo.getApi_config().getHeaders().toString());
                    }
                } else {
                    // 如果headers是其他类型（如字符串），直接设置
                    apiMarketVo.setHeaders(parsedVo.getApi_config().getHeaders().toString());
                }
            }
            
            // 请求体转换 - 需要正确处理从字符串解析的headers和body
            if (parsedVo.getApi_config().getRequest_body() != null) {
                String bodyType = parsedVo.getApi_config().getRequest_body().getBody_type();
                if (bodyType != null && bodyType.equalsIgnoreCase("json")) {
                    apiMarketVo.setBodyType(0); // json
                } else if (bodyType != null && ("form".equalsIgnoreCase(bodyType) || "form-data".equalsIgnoreCase(bodyType))) {
                    apiMarketVo.setBodyType(1); // form-data
                } else if (bodyType != null && ("urlencoded".equalsIgnoreCase(bodyType) || "x-www-form-urlencoded".equalsIgnoreCase(bodyType))) {
                    apiMarketVo.setBodyType(2); // url-encoded
                } else if (bodyType != null && "xml".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(3); // xml
                } else if (bodyType != null && "text".equalsIgnoreCase(bodyType)) {
                    apiMarketVo.setBodyType(4); // text
                } else {
                    apiMarketVo.setBodyType(0); // 默认json
                }
                
                // 从body_template获取请求体内容
                String bodyTemplate = parsedVo.getApi_config().getRequest_body().getBody_template();
                if (bodyTemplate != null) {
                    // 检查bodyTemplate是否是空对象或表示headers的格式
                    if ("{}".equals(bodyTemplate.trim()) || bodyTemplate.trim().isEmpty()) {
                        // 空的body，可能是headers信息被错误地放在了这里
                        apiMarketVo.setBodyTemplate(null);
                    } else {
                        // 检查是否是headers格式
                        if (bodyTemplate.contains("Content-Type") || bodyTemplate.contains("Authorization") || 
                            bodyTemplate.startsWith("{") && bodyTemplate.contains(":")) {
                            // 这可能是headers信息
                            try {
                                Map<String, Object> headersMap = objectMapper.readValue(bodyTemplate, Map.class);
                                apiMarketVo.setHeaders(objectMapper.writeValueAsString(headersMap));
                                apiMarketVo.setBodyTemplate(null); // headers不是body
                            } catch (Exception e) {
                                // 如果不是有效的JSON headers，作为body处理
                                apiMarketVo.setBodyTemplate(bodyTemplate);
                            }
                        } else {
                            // 作为body template处理
                            apiMarketVo.setBodyTemplate(bodyTemplate);
                        }
                    }
                }
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
            apiMarketVo.setPricingModel(0); // 免费
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
        Map<String, Object> varMap = new HashMap<>(); // 用于存储变量映射，使用Object类型以支持复杂对象
        if (parsedVo.getVariables_config() != null && !parsedVo.getVariables_config().isEmpty()) {
            boolean isFirstParam = true;
            for (ApiDocumentParsedVo.VariableConfig varConfig : parsedVo.getVariables_config()) {
                if ("header".equalsIgnoreCase(varConfig.getLocation())) {
                    headersBuilder.append(varConfig.getName()).append(": ").append(varConfig.getDefault_value()).append("\n");
                    // 不将头部变量添加到变量映射中，只设置到headers
                } else if ("url".equalsIgnoreCase(varConfig.getLocation())) {
                    // 处理URL参数 - 这里构建URL参数字符串，但不直接添加到URL，而是依赖变量替换
                    // 将URL参数添加到变量映射中
                    // 优先使用参数说明中的示例值，如果为空则使用默认值，如果默认值也为空则使用占位符
                    String varValue = null;
                    
                    // 从description中提取示例值（如果description包含示例）
                    String description = varConfig.getDescription();
                    if (description != null) {
                        // 尝试从description中提取示例值，例如 "例如奥迪A6L、小米su7" 中的示例
                        varValue = extractExampleFromDescription(description);
                    }
                    
                    // 如果从描述中没有提取到示例值，则使用default_value
                    if (varValue == null || varValue.trim().isEmpty()) {
                        varValue = varConfig.getDefault_value();
                    }
                    
                    // 如果default_value也为空，则使用变量名作为占位符
                    if (varValue == null || varValue.trim().isEmpty()) {
                        varValue = varConfig.getName(); // 使用变量名本身作为占位符，而不是'test_value'
                    }
                    
                    // 确保变量映射中包含URL参数的值
                    Map<String, String> varValueMap = new HashMap<>();
                    varValueMap.put("value", varValue);
                    varValueMap.put("desc", varConfig.getDescription() != null ? varConfig.getDescription() : "");
                    varMap.put(varConfig.getName(), varValueMap);
                } else if ("body".equalsIgnoreCase(varConfig.getLocation())) {
                    // 处理请求体变量，不添加到变量映射中
                }
            }
            
            // 将URL中的 $var 格式转换为 ${var} 格式，以便变量替换逻辑能正确处理
            if (apiMarketVo.getUrl() != null) {
                String url = apiMarketVo.getUrl();
                log.debug("Original URL from parser: {}", url);
                
                // 检查是否包含$字符
                if (url.contains("$")) {
                    log.debug("URL contains $ character, proceeding with replacement");
                } else {
                    log.debug("URL does not contain $ character");
                }
                
                // 将 $var 格式转换为 ${var} 格式
                // 使用正则表达式匹配 $ 后跟字母开头、后跟字母数字下划线的变量名
                Pattern urlVarPattern = Pattern.compile("\\$([a-zA-Z][a-zA-Z0-9_]*)");
                Matcher urlVarMatcher = urlVarPattern.matcher(url);
                StringBuffer sb = new StringBuffer();
                boolean foundMatch = false;
                while (urlVarMatcher.find()) {
                    foundMatch = true;
                    String varName = urlVarMatcher.group(1); // 获取变量名
                    if (varName != null) {
                        log.debug("Found variable in URL: {}", varName);
                        // 使用Matcher.quoteReplacement来正确转义替换字符串
                        urlVarMatcher.appendReplacement(sb, Matcher.quoteReplacement("${" + varName + "}"));
                    }
                }
                urlVarMatcher.appendTail(sb);
                String convertedUrl = sb.toString();
                if (!foundMatch) {
                    log.debug("No variables found in URL for conversion");
                }
                log.debug("URL after $var to ${{var}} conversion: {}", convertedUrl);
                apiMarketVo.setUrl(convertedUrl);
            
                // 确保URL中没有不完整的模板变量，避免RestTemplate解析错误
                String finalUrl = apiMarketVo.getUrl();
                // 移除任何不完整的模板格式，如以$或{结尾的不完整模板
                finalUrl = finalUrl.replaceAll("\\$\\{[^}]*$", "");
                finalUrl = finalUrl.replaceAll("\\{[^}]*$", "");
                finalUrl = finalUrl.replaceAll("\\{\\{[^}]*$", "");
                finalUrl = finalUrl.replaceAll("\\$[a-zA-Z0-9_]*$", "");
                // 特别处理URL参数部分的不完整模板格式
                finalUrl = finalUrl.replaceAll("\\?[^#&]*\\$\\{[^}]*$", "?"); // 处理以不完整模板结尾的查询参数
                finalUrl = finalUrl.replaceAll("\\?[^#&]*\\{[^}]*$", "?"); // 处理以不完整模板结尾的查询参数
                finalUrl = finalUrl.replaceAll("\\?[^#&]*\\{\\{[^}]*$", "?"); // 处理以不完整模板结尾的查询参数
                // 处理URL参数中间的不完整模板
                finalUrl = finalUrl.replaceAll("\\$\\{[^}]*\\&", "&");
                finalUrl = finalUrl.replaceAll("\\{[^}]*\\&", "&");
                finalUrl = finalUrl.replaceAll("\\{\\{[^}]*\\&", "&");
                // 最后确保URL末尾没有多余的符号
                finalUrl = finalUrl.replaceAll("\\?&", "?");
                finalUrl = finalUrl.replaceAll("\\?\\?+", "?");
                log.debug("Final URL after cleaning: {}", finalUrl);
                apiMarketVo.setUrl(finalUrl);
            }
        }
        // 只有当headersBuilder不为空时才设置，否则保留之前解析的headers
        if (headersBuilder.length() > 0) {
            if (apiMarketVo.getHeaders() == null || apiMarketVo.getHeaders().isEmpty()) {
                // 如果当前headers为空，使用从variables_config解析的headers
                apiMarketVo.setHeaders(headersBuilder.toString());
            } else {
                // 如果当前headers不为空，需要将variables_config中的headers合并进去
                String existingHeaders = apiMarketVo.getHeaders();
                // 检查现有headers是否为JSON格式
                if (existingHeaders.trim().startsWith("{")) {
                    try {
                        // 解析现有的JSON格式headers
                        Map<String, Object> existingHeadersMap = objectMapper.readValue(existingHeaders, Map.class);
                        // 解析headersBuilder中的多行格式
                        String[] headerLines = headersBuilder.toString().split("\n");
                        for (String headerLine : headerLines) {
                            headerLine = headerLine.trim();
                            if (!headerLine.isEmpty()) {
                                int colonIndex = headerLine.indexOf(":");
                                if (colonIndex > 0) {
                                    String key = headerLine.substring(0, colonIndex).trim();
                                    String value = headerLine.substring(colonIndex + 1).trim();
                                    existingHeadersMap.put(key, value);
                                }
                            }
                        }
                        // 更新headers
                        apiMarketVo.setHeaders(objectMapper.writeValueAsString(existingHeadersMap));
                    } catch (Exception e) {
                        log.warn("合并headers时解析JSON失败，使用变量配置的headers", e);
                        apiMarketVo.setHeaders(headersBuilder.toString());
                    }
                } else {
                    // 如果不是JSON格式，追加headers
                    apiMarketVo.setHeaders(existingHeaders + "\n" + headersBuilder.toString());
                }
            }
        }
        
        // 将变量映射转换为JSON字符串并设置到varRow字段
        if (!varMap.isEmpty()) {
            try {
                String varRowJson = objectMapper.writeValueAsString(varMap);
                apiMarketVo.setVarRow(varRowJson);
                log.debug("Setting varRow to: {}", varRowJson); // 添加调试日志
            } catch (JsonProcessingException e) {
                log.error("转换变量映射为JSON字符串时发生错误", e);
                // 如果转换失败，使用空字符串
                apiMarketVo.setVarRow("{}");
            }
        } else {
            apiMarketVo.setVarRow("{}");
        }
        
        return apiMarketVo;
    }

    /**
     * 从字符串格式解析API信息，格式为 "url,method,headers,body"
     * @param apiInfoString API信息字符串
     * @return 解析后的ApiDocumentParsedVo对象
     */
    private ApiDocumentParsedVo parseApiInfoFromString(String apiInfoString) {
        log.debug("解析API信息字符串: {}", apiInfoString);
        
        // 创建默认的ApiDocumentParsedVo对象
        ApiDocumentParsedVo parsedVo = new ApiDocumentParsedVo();
        ApiDocumentParsedVo.ApiConfig apiConfig = new ApiDocumentParsedVo.ApiConfig();
        ApiDocumentParsedVo.BasicInfo basicInfo = new ApiDocumentParsedVo.BasicInfo();
        ApiDocumentParsedVo.ApiConfig.RequestBody requestBody = new ApiDocumentParsedVo.ApiConfig.RequestBody();
        
        // 按逗号分割字符串
        String[] parts = apiInfoString.split(",");
        if (parts.length >= 4) {
            // 第一部分是URL
            String url = parts[0].trim();
            apiConfig.setRequest_url(url);
            
            // 第二部分是方法
            String method = parts[1].trim();
            apiConfig.setMethod(method);
            
            // 第三部分是headers
            String headersStr = parts[2].trim();
            // 将headers信息存储到变量配置中或直接设置为headers
            Map<String, String> headersMap = new HashMap<>();
            if (headersStr.startsWith("{") && headersStr.endsWith("}")) {
                try {
                    // 解析JSON格式的headers
                    headersMap = objectMapper.readValue(headersStr, Map.class);
                } catch (Exception e) {
                    log.warn("解析headers JSON失败: {}", e.getMessage());
                    // 如果不是JSON格式，尝试简单解析，如 {"Content-Type":"application/json"}
                    headersStr = headersStr.replaceAll("[{}\"]", ""); // 移除大括号和引号
                    String[] headerParts = headersStr.split(",");
                    for (String headerPart : headerParts) {
                        String[] keyValue = headerPart.split(":");
                        if (keyValue.length == 2) {
                            headersMap.put(keyValue[0].trim(), keyValue[1].trim());
                        }
                    }
                }
            } else {
                // 尝试解析非JSON格式的headers
                headersStr = headersStr.replaceAll("[{}\"]", ""); // 移除可能的大括号和引号
                String[] headerParts = headersStr.split(",");
                for (String headerPart : headerParts) {
                    String[] keyValue = headerPart.split(":");
                    if (keyValue.length == 2) {
                        headersMap.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
                        
            // 将解析的headers设置到ApiConfig中
            try {
                String headersJson = objectMapper.writeValueAsString(headersMap);
                apiConfig.setHeaders(headersMap); // 直接设置为Map对象，让Jackson处理序列化
            } catch (Exception e) {
                log.warn("转换headers为JSON字符串失败: {}", e.getMessage());
                apiConfig.setHeaders(headersStr); // 使用原始字符串
            }
            
            // 第四部分是body
            String bodyStr = parts[3].trim();
            requestBody.setBody_template(bodyStr);
            requestBody.setBody_type("json"); // 默认JSON类型
            
            apiConfig.setRequest_body(requestBody);
            
            // 设置协议
            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                apiConfig.setProtocol("HTTP");
            } else {
                apiConfig.setProtocol("HTTP"); // 默认HTTP
            }
            
            // 设置基本信息
            String apiName = extractApiNameFromUrl(url); // 从URL中提取API名称
            basicInfo.setApi_name(apiName);
            basicInfo.setApi_description("API parsed from document: " + url);
            basicInfo.setEnabled(true);
        } else {
            // 如果格式不符合预期，创建一个基本的ApiDocumentParsedVo对象
            basicInfo.setApi_name("Parsed API");
            basicInfo.setApi_description("API parsed from document");
            basicInfo.setEnabled(true);
            apiConfig.setRequest_url(apiInfoString.trim()); // 将整个字符串作为URL
            apiConfig.setMethod("GET"); // 默认GET
            apiConfig.setProtocol("HTTP"); // 默认HTTP
            apiConfig.setRequest_body(new ApiDocumentParsedVo.ApiConfig.RequestBody());
            //apiConfig.setRequest_body().setBody_type("json");
        }
        
        parsedVo.setBasic_info(basicInfo);
        parsedVo.setApi_config(apiConfig);
        
        log.debug("解析结果: {}", parsedVo);
        return parsedVo;
    }
    
    /**
     * 从URL中提取API名称
     * @param url API URL
     * @return 提取的API名称
     */
    private String extractApiNameFromUrl(String url) {
        try {
            // 从URL中提取最后一个路径段作为API名称
            String path = new java.net.URL(url).getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length > 0) {
                String name = pathParts[pathParts.length - 1];
                if (!name.isEmpty()) {
                    return name;
                }
            }
            // 如果无法从路径提取，使用域名的路径部分
            return "API_" + path.replace("/", "_").replaceFirst("^_", "");
        } catch (Exception e) {
            // 如果URL解析失败，使用默认名称
            return "Parsed_API";
        }
    }

    /**
     * 从参数描述中提取示例值
     * @param description 参数描述
     * @return 提取的示例值，如果没有找到则返回null
     */
    private String extractExampleFromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        // 查找描述中的示例关键词，如"例如"、"如"等
        // 匹配模式：例如 奥迪A6L、小米su7 或 如 奥迪A6L，小米su7
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("例如[\\s\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\.,，。、]+|如[\\s\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\.,，。、]+");
        java.util.regex.Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            String exampleText = matcher.group();
            // 提取关键词后的示例值
            if (exampleText.contains("例如")) {
                exampleText = exampleText.substring(exampleText.indexOf("例如") + 2).trim();
            } else if (exampleText.contains("如")) {
                exampleText = exampleText.substring(exampleText.indexOf("如") + 1).trim();
            }

            // 从示例文本中提取第一个值（通常第一个示例是最合适的测试值）
            String[] examples = exampleText.split("[\\s\\.,，。、]");
            for (String example : examples) {
                example = example.trim();
                if (!example.isEmpty() && !example.equals("、")) {
                    // 去除可能的引号和其他标点
                    example = example.replaceAll("^[\"'\\(\\[\\{\\s]+|[\"'\\)\\]\\}\\s]+$", "");
                    return example;
                }
            }
        }

        return null;
    }

    private ApiTestResultVo analyzeTestFailureWithAI(ApiTestResultVo testResult, ApiMarketVo apiMarketVo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiDocumentParserConfig.getAnalysisApiKey());

            Map<String, Object> payload = new HashMap<>();

            // 1️⃣ 把失败响应体拼成 prompt（关键）
            String query = String.format(
                    "以下是一次API调用的响应结果，请分析API调用是否成功。如果API调用成功，返回 success=true；如果失败，返回 success=false 并给出失败原因。\n\n" +
                            "API名称：%s\n" +
                            "HTTP状态码：%s\n" +
                            "响应体：\n%s",
                    apiMarketVo.getName(),
                    testResult.getStatusCode(),
                    testResult.getResponseBody()
            );

            payload.put("query", query);

            // 2️⃣ inputs 必须是 Map（可以为空）
            payload.put("inputs", new HashMap<>());

            payload.put("response_mode", "blocking");
            payload.put("user", String.valueOf(SecurityUtils.getUserId()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = analysisRestTemplate.postForEntity(
                    apiDocumentParserConfig.getAnalysisUrl(),
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Received response from analysis service: {}", response.getBody());

                String extracted = extractErrorMessageFromAnalysis(response.getBody());
                if (extracted != null && !extracted.isEmpty()) {
                    testResult.setMessage(extracted);
                }
                
                // 根据AI分析的结果来决定success状态
                // 直接解析AI分析返回的完整结构，根据success字段判断
                try {
                    Map<String, Object> analysisResult = objectMapper.readValue(response.getBody(), Map.class);
                    
                    // 检查AI分析结果中是否包含success字段
                    if (analysisResult.containsKey("success")) {
                        boolean aiSuccess = (Boolean) analysisResult.get("success");
                        testResult.setSuccess(aiSuccess);
                    } else if (analysisResult.containsKey("answer")) {
                        // 如果没有success字段，尝试解析answer字段
                        String answerContent = (String) analysisResult.get("answer");
                        // 检查answerContent是否为JSON格式，可能是包含error_info等结构的响应
                        if (answerContent.trim().startsWith("{") && answerContent.trim().endsWith("}")) {
                            try {
                                Map<String, Object> answerObj = objectMapper.readValue(answerContent, Map.class);
                                // 检查是否有success字段
                                if (answerObj.containsKey("success")) {
                                    boolean answerSuccess = (Boolean) answerObj.get("success");
                                    testResult.setSuccess(answerSuccess);
                                } else if (answerObj.containsKey("error_info")) {
                                    // 如果有error_info结构，通常表示失败
                                    testResult.setSuccess(false);
                                } else if (answerObj.containsKey("status")) {
                                    String status = (String) answerObj.get("status");
                                    if ("fail".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                                        testResult.setSuccess(false);
                                    } else if ("success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status)) {
                                        testResult.setSuccess(true);
                                    }
                                }
                            } catch (Exception e) {
                                // 如果answerContent不是JSON格式，继续使用字符串判断
                                if (answerContent.toLowerCase().contains("success") || answerContent.toLowerCase().contains("成功")) {
                                    testResult.setSuccess(true);
                                } else if (answerContent.toLowerCase().contains("fail") || answerContent.toLowerCase().contains("错误") || 
                                           answerContent.toLowerCase().contains("失败") || answerContent.toLowerCase().contains("error")) {
                                    testResult.setSuccess(false);
                                }
                            }
                        } else {
                            // 如果answerContent不是JSON格式，使用字符串判断
                            if (answerContent.toLowerCase().contains("success") || answerContent.toLowerCase().contains("成功")) {
                                testResult.setSuccess(true);
                            } else if (answerContent.toLowerCase().contains("fail") || answerContent.toLowerCase().contains("错误") || 
                                       answerContent.toLowerCase().contains("失败") || answerContent.toLowerCase().contains("error")) {
                                testResult.setSuccess(false);
                            }
                        }
                    } else {
                        // 如果AI分析结果中没有success字段，检查是否有error_info结构
                        if (analysisResult.containsKey("error_info")) {
                            // 根据error_info的存在判断为失败
                            testResult.setSuccess(false);
                        } else if (analysisResult.containsKey("status")) {
                            String status = (String) analysisResult.get("status");
                            if ("fail".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                                testResult.setSuccess(false);
                            } else if ("success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status)) {
                                testResult.setSuccess(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("无法解析AI分析结果来判断成功状态，默认使用当前状态", e);
                }
            }
        } catch (Exception e) {
            log.error("分析API测试响应时发生错误，使用原始错误信息", e);
            testResult.setMessage("API测试失败：" + testResult.getResponseBody());
            // 如果AI分析失败，保持原始测试结果的success状态
        }

        return testResult;
    }


    /**
     * 从分析服务响应中提取错误信息
     * @param analysisResponseBody 分析服务响应体
     * @return 提取的错误信息
     */
    private String extractErrorMessageFromAnalysis(String analysisResponseBody) {
        try {
            // 尝试解析分析结果
            Map<String, Object> analysisResult = objectMapper.readValue(analysisResponseBody, Map.class);
            String analysisContent = null;
            
            // 检查分析结果结构
            if (analysisResult.containsKey("answer")) {
                analysisContent = (String) analysisResult.get("answer");
            } else if (analysisResult.containsKey("text")) {
                analysisContent = (String) analysisResult.get("text");
            } else {
                analysisContent = analysisResponseBody;
            }
            
            // 尝试解析为JSON并提取错误信息
            try {
                Map<String, Object> errorResponse = objectMapper.readValue(analysisContent, Map.class);
                
                // 优先检查是否有error_info结构，以及其中的error_message
                Object errorInfo = errorResponse.get("error_info");
                if (errorInfo instanceof Map) {
                    Map<String, Object> errorInfoMap = (Map<String, Object>) errorInfo;
                    Object errorMessage = errorInfoMap.get("error_message");
                    if (errorMessage != null && !"".equals(errorMessage.toString().trim())) {
                        return errorMessage.toString();
                    }
                }
                
                // 检查直接的error_message字段
                Object directErrorMessage = errorResponse.get("error_message");
                if (directErrorMessage != null && !"".equals(directErrorMessage.toString().trim())) {
                    return directErrorMessage.toString();
                }
                
                // 检查msg字段
                Object msg = errorResponse.get("msg");
                if (msg != null && !"".equals(msg.toString().trim())) {
                    return msg.toString();
                }
                
                // 检查message字段
                Object message = errorResponse.get("message");
                if (message != null && !"".equals(message.toString().trim())) {
                    return message.toString();
                }
                
                // 检查conclusion字段
                Object conclusion = errorResponse.get("conclusion");
                if (conclusion != null && !"".equals(conclusion.toString().trim())) {
                    return conclusion.toString();
                }
                
                // 如果以上都没有，返回整个error_info对象的内容（如果存在）
                if (errorInfo instanceof Map) {
                    return errorInfo.toString();
                }
            } catch (Exception jsonException) {
                log.debug("无法解析分析结果为JSON，使用正则表达式提取: {}", jsonException.getMessage());
                // 如果无法解析为JSON，尝试直接查找错误信息
                if (analysisContent.contains("error_message")) {
                    // 尝试使用正则表达式提取error_message
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"error_message\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = pattern.matcher(analysisContent);
                    if (matcher.find() && !matcher.group(1).isEmpty()) {
                        return matcher.group(1);
                    }
                }
                
                // 如果正则表达式也没有找到error_message，尝试提取conclusion
                java.util.regex.Pattern conclusionPattern = java.util.regex.Pattern.compile("\"conclusion\"\\s*:\\s*\"([^\"]+)\"");
                java.util.regex.Matcher conclusionMatcher = conclusionPattern.matcher(analysisContent);
                if (conclusionMatcher.find() && !conclusionMatcher.group(1).isEmpty()) {
                    return conclusionMatcher.group(1);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse analysis response: {}", e.getMessage());
        }
        
        return null;
    }

    @PostMapping("/test")
    public BaseResponse testApi(@RequestBody ApiMarketVo apiMarketVo) {
        return ResultUtils.success(apiManageBusinessService.testApi(apiMarketVo));
    }
    

}