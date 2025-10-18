package com.kuafuai.api.client;

public interface ApiClientEventHandler {

    WssEventMessage event(String message);
}
