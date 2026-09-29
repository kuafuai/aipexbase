package com.kuafuai.voice.gateway;

import com.kuafuai.voice.handshake.AccessTokenHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音网关的透传 handler。
 *
 * <p>每个进来的 client session 建立一个上游到火山的 OkHttp WebSocket，一一对应。
 * 收到的二进制/文本帧原样双向转发；任一端断开另一端立刻 close。协议帧的解析完全
 * 不做——这正是 body 侧代码可以不动的前提。
 *
 * <p>火山的 ASR 和 TTS 都只发二进制帧，但保留 text 通道以防上游偶发调试消息。
 */
@Slf4j
@Component
public class VoiceProxyHandler extends AbstractWebSocketHandler {

    private static final CloseStatus UPSTREAM_UNAVAILABLE = new CloseStatus(1011, "upstream unavailable");
    private static final CloseStatus UPSTREAM_CLOSED = new CloseStatus(1000, "upstream closed");
    private static final CloseStatus UPSTREAM_ERROR = new CloseStatus(1011, "upstream error");

    /**
     * session.id → upstream socket，用于把客户端发来的帧转出去、以及关闭时清理。
     */
    private final Map<String, WebSocket> upstreams = new ConcurrentHashMap<>();

    private final OkHttpClient httpClient;

    public VoiceProxyHandler(@Qualifier("voiceUpstreamClient") OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String appId = (String) session.getAttributes().get(AccessTokenHandshakeInterceptor.ATTR_APP_ID);
        String upstreamUrl = (String) session.getAttributes().get(AccessTokenHandshakeInterceptor.ATTR_UPSTREAM_URL);
        HttpHeaders upstreamHeaders = (HttpHeaders) session.getAttributes()
                .get(AccessTokenHandshakeInterceptor.ATTR_UPSTREAM_HEADERS);

        if (upstreamUrl == null || upstreamHeaders == null) {
            log.error("voice proxy: session {} missing handshake attributes", session.getId());
            safeClose(session, UPSTREAM_UNAVAILABLE);
            return;
        }

        Request.Builder rb = new Request.Builder().url(upstreamUrl);
        for (Map.Entry<String, List<String>> e : upstreamHeaders.entrySet()) {
            for (String v : e.getValue()) {
                rb.addHeader(e.getKey(), v);
            }
        }

        WebSocket upstream = httpClient.newWebSocket(rb.build(), new UpstreamListener(session, appId));
        upstreams.put(session.getId(), upstream);
        log.info("voice proxy open: session={} appId={} -> {}", session.getId(), appId, upstreamUrl);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        WebSocket upstream = upstreams.get(session.getId());
        if (upstream == null) return;
        ByteBuffer buf = message.getPayload();
        byte[] bytes;
        if (buf.hasArray() && buf.arrayOffset() == 0 && buf.remaining() == buf.array().length) {
            bytes = buf.array();
        } else {
            bytes = new byte[buf.remaining()];
            buf.get(bytes);
        }
        boolean ok = upstream.send(ByteString.of(bytes));
        if (!ok) {
            log.warn("voice proxy: upstream send buffer full, session={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocket upstream = upstreams.get(session.getId());
        if (upstream == null) return;
        upstream.send(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocket upstream = upstreams.remove(session.getId());
        if (upstream != null) {
            try {
                upstream.close(status.getCode(), status.getReason());
            } catch (Throwable t) {
                upstream.cancel();
            }
        }
        log.info("voice proxy closed: session={} code={} reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("voice proxy transport error: session={} err={}", session.getId(), exception.toString());
        WebSocket upstream = upstreams.remove(session.getId());
        if (upstream != null) upstream.cancel();
        safeClose(session, UPSTREAM_ERROR);
    }

    private static void safeClose(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (IOException ignored) {
        }
    }

    private final class UpstreamListener extends WebSocketListener {
        private final WebSocketSession client;
        private final String appId;

        UpstreamListener(WebSocketSession client, String appId) {
            this.client = client;
            this.appId = appId;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.debug("voice upstream open: session={} appId={} status={}", client.getId(), appId, response.code());
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            try {
                if (!client.isOpen()) return;
                synchronized (client) {
                    client.sendMessage(new BinaryMessage(bytes.asByteBuffer()));
                }
            } catch (Throwable t) {
                log.warn("voice proxy: forward to client failed, session={} err={}",
                        client.getId(), t.toString());
                webSocket.cancel();
                safeClose(client, UPSTREAM_ERROR);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                if (!client.isOpen()) return;
                synchronized (client) {
                    client.sendMessage(new TextMessage(text));
                }
            } catch (Throwable t) {
                log.warn("voice proxy: forward text to client failed, session={} err={}",
                        client.getId(), t.toString());
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            upstreams.remove(client.getId());
            log.debug("voice upstream closed: session={} code={} reason={}", client.getId(), code, reason);
            safeClose(client, new CloseStatus(code, reason));
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            upstreams.remove(client.getId());
            log.warn("voice upstream failure: session={} appId={} err={} httpStatus={}",
                    client.getId(), appId, t.toString(), response == null ? "-" : response.code());
            safeClose(client, response == null ? UPSTREAM_UNAVAILABLE : UPSTREAM_CLOSED);
        }
    }
}
