package com.kuafuai.voice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "voice.llm")
public class LlmProperties {

    /** 上游 LLM 平台的 base URL，例如 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    /** 上游平台真实 API key。网关持有，客户端拿到的是 aipexbase 签发的 kft_xxx。 */
    private String apiKey;

    /**
     * 允许客户端使用的模型白名单。空表示不校验。
     * 建议至少填一份显式清单，避免客户端拿一个 voice token 去调平台任意模型。
     */
    private List<String> allowedModels = Collections.emptyList();
}
