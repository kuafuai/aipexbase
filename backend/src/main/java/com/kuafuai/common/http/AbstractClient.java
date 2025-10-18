package com.kuafuai.common.http;

import com.kuafuai.common.exception.BusinessException;
import okhttp3.Headers;
import okhttp3.Response;

import javax.crypto.Mac;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractClient {
    public static final int HTTP_RSP_OK = 200;
    public TCLog log;
    private HttpConnection httpConnection;

    public AbstractClient() {
        this.log = new TCLog();
        this.httpConnection = new HttpConnection(60, 60, 60);
        this.httpConnection.addInterceptors(this.log);

        warmup();
    }

    protected String internalRequest(String url, String method, AbstractModel request) {
        Response okRsp;
        try {
            okRsp = internalRequestRaw(url, method, request);
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        }

        String strResp;
        try {
            strResp = okRsp.body().string();
        } catch (IOException e) {
            String msg = "Cannot transfer response body to string, because Content-Length is too large, or Content-Length and stream length disagree.";
            throw new BusinessException(msg + "\r\n" + e.getMessage());
        }
        return strResp;
    }

    protected Response internalRequestRaw(String url, String method, AbstractModel request) throws BusinessException, IOException {
        Response okRsp = doRequest(url, method, request);

        if (okRsp.code() != AbstractClient.HTTP_RSP_OK) {
            String msg = "response code is " + okRsp.code() + ", not 200";
            throw new BusinessException(msg);
        }
        return okRsp;
    }

    private Response doRequest(String url, String method, AbstractModel request) throws BusinessException, IOException {
        String httpRequestMethod = method;
        String contentType = "application/x-www-form-urlencoded";
        byte[] requestPayload = "".getBytes(StandardCharsets.UTF_8);

        HashMap<String, String> params = new HashMap<String, String>();
        request.toMap(params, "");
        String[] binaryParams = request.getBinaryParams();

        if (binaryParams.length > 0) {
            httpRequestMethod = "POST";
            String boundary = UUID.randomUUID().toString();
            contentType = "multipart/form-data; charset=utf-8" + "; boundary=" + boundary;
            try {
                requestPayload = getMultipartPayload(request, boundary);
            } catch (Exception e) {
                throw new BusinessException("Failed to generate multipart." + e.getMessage());
            }
        } else if (httpRequestMethod.equals("POST")) {
            requestPayload = AbstractModel.toJsonString(request).getBytes(StandardCharsets.UTF_8);
            contentType = "application/json; charset=utf-8";
        }

        String canonicalQueryString = this.getCanonicalQueryString(params, httpRequestMethod);

        Headers.Builder hb = new Headers.Builder();
        hb.add("Content-Type", contentType);
        if (null != request.GetHeader()) {
            for (Map.Entry<String, String> entry : request.GetHeader().entrySet()) {
                hb.add(entry.getKey(), entry.getValue());
            }
        }

        Headers headers = hb.build();
        if (httpRequestMethod.equals("GET")) {
            return this.httpConnection.getRequest(url + "?" + canonicalQueryString, headers);
        } else if (httpRequestMethod.equals("POST")) {
            return this.httpConnection.postRequest(url + "?" + canonicalQueryString, requestPayload, headers);
        } else {
            throw new BusinessException("Method only support GET, POST");
        }
    }

    private String getCanonicalQueryString(HashMap<String, String> params, String method)
            throws BusinessException {
//        if (method != null && method.equals("POST")) {
//            return "";
//        }
        StringBuilder queryString = new StringBuilder("");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String v;
            try {
                v = URLEncoder.encode(entry.getValue(), "UTF8");
            } catch (UnsupportedEncodingException e) {
                throw new BusinessException("UTF8 is not supported." + e.getMessage());
            }
            queryString.append("&").append(entry.getKey()).append("=").append(v);
        }
        if (queryString.length() == 0) {
            return "";
        } else {
            return queryString.toString().substring(1);
        }
    }

    private byte[] getMultipartPayload(AbstractModel request, String boundary) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String[] binaryParams = request.getBinaryParams();
        for (Map.Entry<String, byte[]> entry : request.getMultipartRequestParams().entrySet()) {
            baos.write("--".getBytes(StandardCharsets.UTF_8));
            baos.write(boundary.getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Disposition: form-data; name=\"".getBytes(StandardCharsets.UTF_8));
            baos.write(entry.getKey().getBytes(StandardCharsets.UTF_8));
            if (Arrays.asList(binaryParams).contains(entry.getKey())) {
                baos.write("\"; filename=\"".getBytes(StandardCharsets.UTF_8));
                baos.write(entry.getKey().getBytes(StandardCharsets.UTF_8));
                baos.write("\"\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                baos.write("\"\r\n".getBytes(StandardCharsets.UTF_8));
            }
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(entry.getValue());
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        if (baos.size() != 0) {
            baos.write("--".getBytes(StandardCharsets.UTF_8));
            baos.write(boundary.getBytes(StandardCharsets.UTF_8));
            baos.write("--\r\n".getBytes(StandardCharsets.UTF_8));
        }
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    private void warmup() {
        try {
            // it happens in SDK signature process.
            // first invoke costs around 250 ms.
            Mac.getInstance("HmacSHA1");
            Mac.getInstance("HmacSHA256");
            // it happens inside okhttp, but I think any https framework/package will do the same.
            // first invoke costs around 150 ms.
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (Exception e) {
            // ignore but print message to console

        }
    }
}
