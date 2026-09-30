package com.kuafuai.voice.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.kuafuai.accesstoken.entity.AppAccessToken;
import com.kuafuai.accesstoken.service.AppAccessTokenService;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.voice.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Slf4j
@RestController
@RequestMapping("/voice/llm")
public class LlmProxyController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX = "kft_";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

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
        long startMs = System.currentTimeMillis();
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
            relay(resp, token.getAppId(), body.length(), startMs, response);
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
     * 边转发边攒全文，结束后从全文里抽 usage。
     * 非流式：usage 在顶层 JSON；流式：usage 在最后一个 data chunk 里。
     */
    private void relay(Response resp, String appId, int reqBytes, long startMs, HttpServletResponse out) throws IOException {
        ResponseBody rb = resp.body();
        int status = resp.code();
        String contentType = "application/octet-stream";
        if (rb != null && rb.contentType() != null) {
            contentType = rb.contentType().toString();
        }

        out.setStatus(status);
        out.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        out.setHeader("Cache-Control", "no-cache");
        out.setHeader("X-Accel-Buffering", "no");

        if (rb == null) return;

        java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream(4096);
        try (java.io.InputStream in = rb.byteStream(); java.io.OutputStream client = out.getOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                client.write(buf, 0, n);
                client.flush();
                sink.write(buf, 0, n);
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        String fullText = sink.toString("UTF-8");
        TokenUsage usage = extractUsage(fullText, contentType);

        log.info("VOICE_METERING channel=llm appId={} status={} durationMs={} reqBytes={} respBytes={} promptTok={} completionTok={} totalTok={}", appId, status, durationMs, reqBytes, sink.size(), usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private static TokenUsage extractUsage(String body, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("event-stream")) {
            return extractSseUsage(body);
        } else {
            return extractJsonUsage(body);
        }
    }

    private static TokenUsage extractJsonUsage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonObject usage = json.getAsJsonObject("usage");
            if (usage == null) {
                return TokenUsage.ZERO;
            }

            return new TokenUsage(safeInt(usage.get("prompt_tokens")), safeInt(usage.get("completion_tokens")), safeInt(usage.get("total_tokens")));
        } catch (JsonParseException e) {
            return TokenUsage.ZERO;
        }
    }

    private static TokenUsage extractSseUsage(String body) {
        String[] lines = body.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();

            if (!line.startsWith("data:")) {
                continue;
            }

            String data = line.substring(5).trim();

            if ("[DONE]".equals(data)) {
                continue;
            }

            int usagePos = data.indexOf("\"usage\"");
            if (usagePos < 0) {
                continue;
            }

            try {
                JsonObject chunk = JsonParser.parseString(data).getAsJsonObject();
                JsonObject usage = chunk.getAsJsonObject("usage");

                if (usage == null) {
                    continue;
                }

                return new TokenUsage(safeInt(usage.get("prompt_tokens")), safeInt(usage.get("completion_tokens")), safeInt(usage.get("total_tokens")));
            } catch (JsonParseException e) {
                log.warn("Failed to parse SSE usage chunk");
            }
        }
        return TokenUsage.ZERO;
    }


    private static int safeInt(com.google.gson.JsonElement e) {
        return (e == null || e.isJsonNull()) ? 0 : e.getAsInt();
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
