package com.kuafuai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "api.document.parser")
public class ApiDocumentParserConfig {
    private String url;
    private String apiKey;
    private String analysisUrl;
    private String analysisApiKey;
    
    @Bean("apiDocumentParserRestTemplate")
    public RestTemplate apiDocumentParserRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        // 连接超时时间设置为10秒
        factory.setConnectTimeout(10000);
        // 读取超时时间设置为60秒（因为文档解析可能需要较长时间）
        factory.setReadTimeout(180000);
        return new RestTemplate(factory);
    }
    
    @Bean("apiAnalysisRestTemplate")
    public RestTemplate apiAnalysisRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        // 连接超时时间设置为10秒
        factory.setConnectTimeout(10000);
        // 读取超时时间设置为60秒（因为分析可能需要较长时间）
        factory.setReadTimeout(180000);
        return new RestTemplate(factory);
    }
}