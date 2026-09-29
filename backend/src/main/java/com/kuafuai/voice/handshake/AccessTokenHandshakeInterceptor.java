package com.kuafuai.voice.handshake;

import com.kuafuai.accesstoken.entity.AppAccessToken;
import com.kuafuai.accesstoken.service.AppAccessTokenService;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.voice.config.HuoshanProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * 语音网关的 WebSocket handshake 鉴权。
 *
 * <p>协议上完全复用 body 侧写死的火山头位——客户端把 aipexbase 签发的 {@code kft_xxx}
 * 放在 {@code X-Api-Key} 里发过来（同一位置本来放的是火山真实 key）。这样 body 端一行
 * 协议代码都不用改，只在配置里把 apiKey 换成平台 token 即可。
 *
 * <p>校验通过后把 appId + 上游需要透传的头都塞进 attributes，交给
 * {@link com.kuafuai.voice.gateway.VoiceProxyHandler} 使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_APP_ID = "voice.appId";
    public static final String ATTR_UPSTREAM_HEADERS = "voice.upstreamHeaders";
    public static final String ATTR_UPSTREAM_URL = "voice.upstreamUrl";

    private static final String HEADER_API_KEY = "X-Api-Key";
    private static final String HEADER_RESOURCE = "X-Api-Resource-Id";
    private static final String HEADER_REQUEST_ID = "X-Api-Request-Id";
    private static final String HEADER_CONNECT_ID = "X-Api-Connect-Id";
    private static final String HEADER_SEQUENCE = "X-Api-Sequence";
    private static final String HEADER_USAGE_RETURN = "X-Control-Require-Usage-Tokens-Return";
    private static final String TOKEN_PREFIX = "kft_";

    private static final AntPathMatcher PATH = new AntPathMatcher();
    private static final String ASR_PATH_PREFIX = "/voice/asr/";
    private static final String TTS_PATH_PREFIX = "/voice/tts/";

    private final AppAccessTokenService accessTokenService;
    private final HuoshanProperties props;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        HttpHeaders headers = request.getHeaders();
        String rawKey = firstNonEmpty(
                headers.getFirst(HEADER_API_KEY),
                stripBearer(headers.getFirst(HttpHeaders.AUTHORIZATION))
        );

        if (StringUtils.isEmpty(rawKey) || !rawKey.startsWith(TOKEN_PREFIX)) {
            log.warn("voice handshake rejected: missing/invalid X-Api-Key from {}", clientAddr(request));
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        AppAccessToken token = accessTokenService.getByToken(rawKey);
        if (token == null) {
            log.warn("voice handshake rejected: unknown token from {}", clientAddr(request));
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String resource = headers.getFirst(HEADER_RESOURCE);
        String upstreamUrl = resolveUpstream(request, resource, response);
        if (upstreamUrl == null) {
            return false;
        }

        HttpHeaders upstream = new HttpHeaders();
        upstream.set(HEADER_API_KEY, props.getApiKey());
        copyIfPresent(headers, upstream, HEADER_RESOURCE);
        copyIfPresent(headers, upstream, HEADER_REQUEST_ID);
        copyIfPresent(headers, upstream, HEADER_CONNECT_ID);
        copyIfPresent(headers, upstream, HEADER_SEQUENCE);
        copyIfPresent(headers, upstream, HEADER_USAGE_RETURN);

        attributes.put(ATTR_APP_ID, token.getAppId());
        attributes.put(ATTR_UPSTREAM_HEADERS, upstream);
        attributes.put(ATTR_UPSTREAM_URL, upstreamUrl);
        log.info("voice handshake ok: appId={} -> upstream={}", token.getAppId(), upstreamUrl);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
    

    /**
     * 客户端连的是 /voice/asr/bigmodel_async，映射到 {asrBase}/bigmodel_async。
     * TTS 同理。query string 一并透传。
     */
    private String resolveUpstream(ServerHttpRequest request, String resource, ServerHttpResponse response) {
        String uri = request.getURI().getPath();
        String base;
        List<String> allowedResources;
        String tail;

        if (uri.startsWith(ASR_PATH_PREFIX)) {
            base = props.getAsrBase();
            allowedResources = props.getAllowedAsrResources();
            tail = uri.substring(ASR_PATH_PREFIX.length());
        } else if (uri.startsWith(TTS_PATH_PREFIX)) {
            base = props.getTtsBase();
            allowedResources = props.getAllowedTtsResources();
            tail = uri.substring(TTS_PATH_PREFIX.length());
        } else {
            log.warn("voice handshake rejected: unrecognised path {}", uri);
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return null;
        }

        if (allowedResources != null && !allowedResources.isEmpty()) {
            if (StringUtils.isEmpty(resource) || !allowedResources.contains(resource)) {
                log.warn("voice handshake rejected: resource {} not in whitelist for {}", resource, uri);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return null;
            }
        }

        String query = request.getURI().getRawQuery();
        String url = trimTrailingSlash(base) + "/" + tail;
        if (StringUtils.isNotEmpty(query)) {
            url = url + "?" + query;
        }
        return url;
    }

    private static void copyIfPresent(HttpHeaders src, HttpHeaders dst, String name) {
        String v = src.getFirst(name);
        if (StringUtils.isNotEmpty(v)) {
            dst.set(name, v);
        }
    }

    private static String firstNonEmpty(String a, String b) {
        return StringUtils.isNotEmpty(a) ? a : b;
    }

    private static String stripBearer(String v) {
        if (v == null) return null;
        String prefix = "Bearer ";
        return v.startsWith(prefix) ? v.substring(prefix.length()) : v;
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String clientAddr(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            return ((ServletServerHttpRequest) request).getServletRequest().getRemoteAddr();
        }
        return "unknown";
    }
}
