package com.kuafuai.api.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.api.util.ApiUtil;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class ApiWssClient {

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    private final Type return_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    public ApiWssClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();
    }


    public Map<String, Object> call(ApiDefinition apiDef, Map<String, Object> params, ApiClientEventHandler eventHandler) {
        try {
            Map<String, String> templateMaps = params.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()
                    ));

            String urlWithParams = ApiUtil.interpolateString(apiDef.url, templateMaps);

            Request request = new Request.Builder().url(urlWithParams).build();
            CompletableFuture<String> future = new CompletableFuture<>();
            CustomWebSocketListener webSocketListener = new CustomWebSocketListener(apiDef, templateMaps, eventHandler, future);

            httpClient.newWebSocket(request, webSocketListener);

            String result = future.get(120, TimeUnit.SECONDS);
            return gson.fromJson(result, return_value_type);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    public static class CustomWebSocketListener extends WebSocketListener {

        private final Map<String, String> params;
        private final ApiDefinition apiDef;

        private final ApiClientEventHandler eventHandler;
        private final CompletableFuture<String> future;

        public CustomWebSocketListener(ApiDefinition apiDef, Map<String, String> params, ApiClientEventHandler eventHandler,
                                       CompletableFuture<String> future) {
            this.apiDef = apiDef;
            this.params = params;
            this.eventHandler = eventHandler;
            this.future = future;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            log.info("wss open connection");
            CustomThread sendThread = new CustomThread(webSocket, this.apiDef, this.params);
            sendThread.start();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            WssEventMessage message = eventHandler.event(text);
            if (message.isFlag()) {
                webSocket.close(1000, "");
                future.complete(message.getContent());
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            log.info("wss close ");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
        }
    }

    public static class CustomThread extends Thread {
        private WebSocket webSocket;
        private Map<String, String> params;
        private ApiDefinition apiDef;

        public CustomThread(WebSocket webSocket, ApiDefinition apiDef, Map<String, String> params) {
            this.webSocket = webSocket;
            this.apiDef = apiDef;
            this.params = params;
        }

        @Override
        public void run() {
            if ("template".equalsIgnoreCase(apiDef.bodyType)) {
                // 通过 模版 转换
                String rendered = ApiUtil.interpolateString(apiDef.bodyTemplate, this.params);
                this.webSocket.send(rendered);
            } else {
                this.webSocket.send(JSON.toJSONString(this.params));
            }
        }
    }

}
