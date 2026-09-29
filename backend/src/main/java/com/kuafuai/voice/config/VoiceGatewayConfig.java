package com.kuafuai.voice.config;

import com.kuafuai.voice.gateway.VoiceProxyHandler;
import com.kuafuai.voice.handshake.AccessTokenHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 语音网关的端点注册。
 *
 * <p>OkHttp 上游客户端的 bean 定义在 {@link VoiceUpstreamClientConfig}——不能放这里，
 * 否则 VoiceGatewayConfig → VoiceProxyHandler → voiceUpstreamClient(defined in gateway)
 * 会构成循环依赖。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class VoiceGatewayConfig implements WebSocketConfigurer {

    private final VoiceProxyHandler proxyHandler;
    private final AccessTokenHandshakeInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(proxyHandler, "/voice/asr/**", "/voice/tts/**")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
