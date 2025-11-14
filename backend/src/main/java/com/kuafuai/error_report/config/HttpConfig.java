package com.kuafuai.error_report.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpConfig {


    @Bean
    public RestTemplate restTemplate() {

        // 1. 连接池管理器
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200); // 最大连接数
        cm.setDefaultMaxPerRoute(50); // 单路由最大连接
        cm.setValidateAfterInactivity(5000); // 5秒无活动后校验连接是否可用 (防止连接复用导致 reset)

        // 2. 配置 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .evictIdleConnections(10, TimeUnit.SECONDS) // 定时清理空闲连接
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true)) // 自动重试 3 次
                .build();

        // 3. 工厂
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        return new RestTemplate(factory);
    }
}
