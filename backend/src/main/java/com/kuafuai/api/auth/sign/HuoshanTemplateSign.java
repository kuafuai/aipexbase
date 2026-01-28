package com.kuafuai.api.auth.sign;


import com.kuafuai.api.auth.entity.HuoshanSignResult;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HuoshanTemplateSign {
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String ADDR = "https://icp.volcengineapi.com";

    public static final String PATH = "/";
    private static final String REGION = "cn-north-1";


    /**
     * 获取sign参数
     */
    public static HuoshanSignResult signResult(String method, Map<String, String> queries, String body,
                                               String action, String version, String service, String accessId, String secretKey)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        // 1. 处理查询参数，添加Action和Version
        Map<String, String> queryParams = new HashMap<>(queries);
        queryParams.put("Action", action);
        queryParams.put("Version", version);

        // 构建请求地址
        String queryString = queryParams.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()) +
                                "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("&"));
        queryString = queryString.replace("+", "%20");
        String requestAddr = ADDR + "?" + queryString;
        System.out.println("request addr: " + requestAddr + "\n");

        // 2. 构建签名核心材料 (与之前版本相同)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date());
        String authDate = date.substring(0, 8);
        byte[] bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        String payload = bytesToHex(hashSha256(bodyBytes));
        List<String> signedHeaders = Arrays.asList("host", "x-date", "x-content-sha256", "content-type");
        String host = URI.create(ADDR).getHost();
        List<String> headerList = new ArrayList<>();
        for (String h : signedHeaders) {
            switch (h) {
                case "host":
                    headerList.add(h + ":" + host);
                    break;
                case "x-date":
                    headerList.add(h + ":" + date);
                    break;
                case "x-content-sha256":
                    headerList.add(h + ":" + payload);
                    break;
                case "content-type":
                    headerList.add(h + ":application/json; charset=utf-8");
                    break;
            }
        }
        String headerString = String.join("\n", headerList);
        String canonicalString = String.join("\n",
                method.toUpperCase(), PATH, queryString, headerString + "\n",
                String.join(";", signedHeaders), payload);
        String hashedCanonicalString = bytesToHex(hashSha256(canonicalString.getBytes(StandardCharsets.UTF_8)));
        String credentialScope = authDate + "/" + REGION + "/" + service + "/request";
        String signString = String.join("\n", "HMAC-SHA256", date, credentialScope, hashedCanonicalString);

        // 3. 生成签名和Authorization头
        byte[] signedKey = getSignedKey(secretKey, authDate, REGION, service);
        String signature = bytesToHex(hmacSha256(signedKey, signString));
        String authorization = String.format(
                "HMAC-SHA256 Credential=%s/%s, SignedHeaders=%s, Signature=%s",
                accessId, credentialScope, java.lang.String.join(";", signedHeaders), signature);


        return HuoshanSignResult.builder()
                .xDate(date)
                .xContentSha256(payload)
                .authorization(authorization)
                .body(new String(body.getBytes(), UTF_8))
                .build();
    }


    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1)
                sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 生成签名密钥链
     */
    private static byte[] getSignedKey(String secretKey, String date, String region, String service)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] kDate = hmacSha256(secretKey.getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "request");
    }


    /**
     * HMAC-SHA256加密
     */
    private static byte[] hmacSha256(byte[] key, String content) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * SHA256哈希
     */
    private static byte[] hashSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

}