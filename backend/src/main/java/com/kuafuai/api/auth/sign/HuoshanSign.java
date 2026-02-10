package com.kuafuai.api.auth.sign;

import com.kuafuai.api.auth.entity.HuoshanSignResult;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class HuoshanSign {

    private static final BitSet URLENCODER = new BitSet(256);
    private static final String CONST_ENCODE = "0123456789ABCDEF";
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static final String PATH = "/";
    public static final String SCHEMA = "https";


    static {
        int i;
        for (i = 97; i <= 122; ++i) {
            URLENCODER.set(i);
        }
        for (i = 65; i <= 90; ++i) {
            URLENCODER.set(i);
        }
        for (i = 48; i <= 57; ++i) {
            URLENCODER.set(i);
        }
        URLENCODER.set('-');
        URLENCODER.set('_');
        URLENCODER.set('.');
        URLENCODER.set('~');
    }

    public static HuoshanSignResult signResult(String accessKey, String secretKey,
                                               String method, Map<String, String> queryList, byte[] body,
                                               Date date,
                                               String action, String version,
                                               String host, String service,
                                               String region) throws Exception {

        if (body == null) {
            body = new byte[0];
        }

        // 1. 计算 body 的 SHA256
        String xContentSha256 = hashSHA256(body);

        // 2. 生成时间戳
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String xDate = sdf.format(date);
        String shortXDate = xDate.substring(0, 8);

        // ========== 关键修改 2：使用正确的 Content-Type ==========
        String contentType = "application/json; charset=utf-8"; // 必须是 application/json

        // 3. 签名的 Header
        String signHeader = "host;x-date;x-content-sha256;content-type";

        // 4. 构建 Query 参数（只需要 Action 和 Version）
        SortedMap<String, String> realQueryList = new TreeMap<>(queryList);
        realQueryList.put("Action", action);
        realQueryList.put("Version", version);

        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key))
                    .append("=")
                    .append(signStringEncoder(realQueryList.get(key)))
                    .append("&");
        }
        if (querySB.length() > 0) {
            querySB.deleteCharAt(querySB.length() - 1);
        }

        // 5. 构建规范请求字符串
        String canonicalStringBuilder = method + "\n" + PATH + "\n" + querySB.toString() + "\n" +
                "host:" + host + "\n" +
                "x-date:" + xDate + "\n" +
                "x-content-sha256:" + xContentSha256 + "\n" +
                "content-type:" + contentType + "\n" +
                "\n" +
                signHeader + "\n" +
                xContentSha256;


        // 6. 计算规范请求的哈希
        String hashcanonicalString = hashSHA256(canonicalStringBuilder.getBytes(UTF_8));

        // 7. 构建签名字符串
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;

        // 8. 生成签名密钥和签名
        byte[] signKey = genSigningSecretKeyV4(secretKey, shortXDate, region, service);
        String signature = bytesToHex(hmacSHA256(signKey, signString));

        // 9. 构建 Authorization Header
        String authorization = "HMAC-SHA256 Credential=" + accessKey + "/" + credentialScope +
                ", SignedHeaders=" + signHeader +
                ", Signature=" + signature;

        // 10. 构建完整的 URL
        String urlStr = SCHEMA + "://" + host + PATH + (querySB.length() > 0 ? "?" + querySB.toString() : "");
        log.info("🐚 成功的cURL命令（直接复制运行）:");
        log.info("========================================");

        // 构建cURL命令
        String curlCommand = String.format(
                "curl -X POST \\\n" +
                        "  '%s' \\\n" +
                        "  -H 'Host: %s' \\\n" +
                        "  -H 'X-Date: %s' \\\n" +
                        "  -H 'X-Content-Sha256: %s' \\\n" +
                        "  -H 'Content-Type: %s' \\\n" +
                        "  -H 'Authorization: %s' \\\n" +
                        "  -d '%s'",
                urlStr, host, xDate, xContentSha256, contentType, authorization,
                new String(body, UTF_8)  // 请求体
        );

        log.info(curlCommand);
        log.info("========================================");
        HuoshanSignResult build = HuoshanSignResult.builder()
                .xDate(xDate)
                .xContentSha256(xContentSha256)
                .authorization(authorization)
                .body(new String(body, UTF_8))
                .build();
        return build;
    }


    private static String signStringEncoder(String source) {
        if (source == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(source.length());
        ByteBuffer bb = UTF_8.encode(source);
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (URLENCODER.get(b)) {
                buf.append((char) b);
            } else if (b == 32) {
                buf.append("%20");
            } else {
                buf.append("%");
                char hex1 = CONST_ENCODE.charAt(b >> 4);
                char hex2 = CONST_ENCODE.charAt(b & 15);
                buf.append(hex1);
                buf.append(hex2);
            }
        }
        return buf.toString();
    }

    public static String hashSHA256(byte[] content) throws Exception {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(content));
        } catch (Exception e) {
            throw new Exception(
                    "Unable to compute hash while signing request: "
                            + e.getMessage(), e);
        }
    }

    public static byte[] hmacSHA256(byte[] key, String content) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            // ========== 关键修改 3：添加字符集指定 ==========
            return mac.doFinal(content.getBytes(UTF_8));
        } catch (Exception e) {
            throw new Exception(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    private static byte[] genSigningSecretKeyV4(String secretKey, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256(secretKey.getBytes(UTF_8), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "request");
    }


    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


}
