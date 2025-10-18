package com.kuafuai.common.http;


import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class TCLog implements Interceptor {

    public void info(String str) {
        log.info(str);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String req = ("send request, request url: " + request.url() + ". request headers information: " + request.headers().toString());
        req = req.replaceAll("\n", ";");
        log.info(req);

        Response response = chain.proceed(request);
        String resp = ("recieve response, response url: " + response.request().url() + ", response headers: " + response.headers().toString() + ",response body information.");
        resp = resp.replaceAll("\n", ";");
        log.info(resp);
        return response;
    }
}
