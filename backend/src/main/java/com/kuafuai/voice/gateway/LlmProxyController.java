package com.kuafuai.voice.gateway;

import com.kuafuai.accesstoken.entity.AppAccessToken;
import com.kuafuai.accesstoken.service.AppAccessTokenService;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.voice.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * LLM 网关：把 OpenAI 兼容的 /chat/completions 请求转发到上游平台（DeepSeek 等）。
 *
 * <p>客户端把 aipexbase 签发的 {@code kft_xxx} 放在 {@code Authorization: Bearer kft_xxx}
 * 里发过来——同一位置本来放的是上游平台真实 key。网关校验通过后把 Authorization 头
 * 换成 {@link LlmProperties#getApiKey()} 转发。这样 body 侧的 {@code LlmClient.kt} 一行
 * 都不用改，只在偏好里把 llm_base_url 指向 aipexbase、把 llm_api_key 换成 kft_xxx 即可。
 *
 * <p>为什么直接写 {@link HttpServletResponse} 而不返回 {@code ResponseEntity}：SSE 场景
 * 下 {@code ResponseEntity<?>} 的通配符让 Spring 挑不出 StreamingResponseBody 的
 * ReturnValueHandler，落到普通 message converter 报 "No converter for lambda"。写
 * response.getOutputStream() 是流式和非流式两条链路共用一条实现，Spring 完全不参与
 * 内容协商。
 */
@Slf4j
@RestController
@RequestMapping("/voice/llm")
public class LlmProxyController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX = "kft_";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final AntPathMatcher PATH = new AntPathMatcher();

    private final AppAccessTokenService accessTokenService;
    private final LlmProperties props;
    private final OkHttpClient http;

    public LlmProxyController(AppAccessTokenService accessTokenService,
                              LlmProperties props,
                              @Qualifier("voiceLlmUpstreamClient") OkHttpClient http) {
        this.accessTokenService = accessTokenService;
        this.props = props;
        this.http = http;
    }

    @PostMapping("/chat/completions")
    public void chatCompletions(@org.springframework.web.bind.annotation.RequestBody String body,
                                @RequestHeader HttpHeaders headers,
                                HttpServletResponse response) throws IOException {
        AppAccessToken token = authenticate(headers);
        if (token == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "invalid or missing kft_ token");
            return;
        }

        String upstreamUrl = trimTrailingSlash(props.getBaseUrl()) + "/chat/completions";
        String accept = headers.getFirst(HttpHeaders.ACCEPT);
        Request upstream = new Request.Builder()
                .url(upstreamUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + props.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.ACCEPT, StringUtils.isEmpty(accept) ? "application/json" : accept)
                .post(okhttp3.RequestBody.create(JSON_MEDIA, body))
                .build();

        try (Response resp = http.newCall(upstream).execute()) {
            relay(resp, token.getAppId(), response);
        } catch (IOException e) {
            log.warn("llm proxy upstream failed: appId={} err={}", token.getAppId(), e.toString());
            writeError(response, HttpStatus.BAD_GATEWAY, "upstream error: " + e.getMessage());
        }
    }

    private AppAccessToken authenticate(HttpHeaders headers) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (StringUtils.isNotEmpty(auth) && auth.startsWith(BEARER_PREFIX)) {
            token = auth.substring(BEARER_PREFIX.length()).trim();
        }
        if (StringUtils.isEmpty(token)) {
            token = headers.getFirst("X-Api-Key");
        }
        if (StringUtils.isEmpty(token) || !token.startsWith(TOKEN_PREFIX)) return null;
        return accessTokenService.getByToken(token);
    }

    /**
     * 把上游响应原样搬到 servlet response 上。SSE 走边读边写，非流式也一样——差别只在
     * flush 的频率，逻辑一条。上游错误响应也 forward，客户端能拿到 DeepSeek 的原始
     * 错误信息（bad key、model not found、context overflow 都在 body 里）。
     */
    private void relay(Response resp, String appId, HttpServletResponse out) throws IOException {
        ResponseBody rb = resp.body();
        int status = resp.code();
        String contentType = "application/octet-stream";
        if (rb != null && rb.contentType() != null) {
            contentType = rb.contentType().toString();
        }

        out.setStatus(status);
        out.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        // SSE 需要，非流式加了无害——省一个 if。X-Accel-Buffering 是给 nginx 的
        // 反向代理提示，客户端和 chat completions 的普通 JSON 都不需要缓冲。
        out.setHeader("Cache-Control", "no-cache");
        out.setHeader("X-Accel-Buffering", "no");

        if (rb == null) return;

        long total = 0;
        try (InputStream in = rb.byteStream(); OutputStream sink = out.getOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                sink.write(buf, 0, n);
                sink.flush();
                total += n;
            }
        }
        if (resp.isSuccessful()) {
            log.info("llm proxy ok: appId={} status={} bytes={}", appId, status, total);
        } else {
            log.warn("llm proxy upstream {} appId={} bytes={}", status, appId, total);
        }
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String msg) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json; charset=utf-8");
        byte[] payload = ("{\"error\":{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}}")
                .getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(payload);
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
