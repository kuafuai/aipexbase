package com.kuafuai.common.http;

import com.kuafuai.common.exception.BusinessException;
import okhttp3.*;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class HttpConnection {

    private static final OkHttpClient clientSingleton = new OkHttpClient();
    private OkHttpClient client;

    public HttpConnection(Integer connTimeout, Integer readTimeout, Integer writeTimeout) {
        this.client = clientSingleton.newBuilder()
                .connectTimeout(connTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }

    public void addInterceptors(Interceptor interceptor) {
        this.client = this.client.newBuilder().addInterceptor(interceptor).build();
    }

    public void setProxy(Proxy proxy) {
        this.client = this.client.newBuilder().proxy(proxy).build();
    }

    public void setProxyAuthenticator(Authenticator authenticator) {
        this.client = this.client.newBuilder().proxyAuthenticator(authenticator).build();
    }

    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
        this.client = this.client.newBuilder().sslSocketFactory(sslSocketFactory, trustManager).build();
    }

    public Response doRequest(Request request) throws IOException {
        return this.client.newCall(request).execute();
    }

    public Response getRequest(String url) throws BusinessException, IOException {
        Request request = null;
        try {
            request = new Request.Builder().url(url).get().build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getClass().getName() + "-" + e.getMessage());
        }

        return this.doRequest(request);
    }

    public Response getRequest(String url, Headers headers) throws BusinessException, IOException {
        Request request = null;
        try {
            request = new Request.Builder().url(url).headers(headers).get().build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getClass().getName() + "-" + e.getMessage());
        }

        return this.doRequest(request);
    }

    public Response postRequest(String url, String body) throws BusinessException, IOException {
        MediaType contentType = MediaType.parse("application/x-www-form-urlencoded");
        Request request = null;
        try {
            request = new Request.Builder().url(url).post(RequestBody.create(contentType, body)).build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getClass().getName() + "-" + e.getMessage());
        }

        return this.doRequest(request);
    }

    public Response postRequest(String url, String body, Headers headers)
            throws BusinessException, IOException {
        MediaType contentType = MediaType.parse(headers.get("Content-Type"));
        Request request = null;
        try {
            request =
                    new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(contentType, body))
                            .headers(headers)
                            .build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getClass().getName() + "-" + e.getMessage());
        }

        return this.doRequest(request);
    }

    public Response postRequest(String url, byte[] body, Headers headers)
            throws BusinessException, IOException {
        MediaType contentType = MediaType.parse(headers.get("Content-Type"));
        Request request = null;
        try {
            request =
                    new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(contentType, body))
                            .headers(headers)
                            .build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getClass().getName() + "-" + e.getMessage());
        }

        return this.doRequest(request);
    }

}

