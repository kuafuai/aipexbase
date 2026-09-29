package com.kuafuai.voice.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 语音网关到上游的 OkHttp 客户端。
 *
 * <p>单独放一个 config，避免和 {@link VoiceGatewayConfig} 挤在一起——后者通过构造函数
 * 注入 {@code VoiceProxyHandler}，而 handler 又依赖这里的 bean，放在同一个类里会形成
 * 循环依赖。
 */
@Configuration
public class VoiceUpstreamClientConfig {

    /**
     * ASR/TTS 走 WebSocket 长连接：readTimeout=0，默认 10s 会把火山连接直接断掉；
     * pingInterval 保活。
     */
    @Bean("voiceUpstreamClient")
    public OkHttpClient voiceUpstreamClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * LLM 走普通 HTTP POST：readTimeout 拉到 3 分钟，thinking 模型答一轮要以分钟计；
     * connect 保持短，网断了要立刻告诉客户端。不打 ping（非长连接）。
     */
    @Bean("voiceLlmUpstreamClient")
    public OkHttpClient voiceLlmUpstreamClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
