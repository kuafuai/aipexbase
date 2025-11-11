package com.kuafuai.error_report.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class SignatureUtils {

    /**
     * Generates a signature for the given data
     * @param data The data parameters (key-value pairs)
     * @param secretKey The secret key for signing
     * @param timestamp The timestamp
     * @return The generated signature in uppercase
     */
    public static String generateSignature(Map<String, Object> data, String secretKey, long timestamp) {
        // Sort parameters by key
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        if (data != null) {
            sortedParams.putAll(data);
        }

        // Build parameter string
        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {

//          如果为空使用 None 字符串进行替换
            Object value = entry.getValue();
            if (value == null){
                value= "None";
            }
            paramString.append(entry.getKey()).append("=").append(value).append("&");
        }

        paramString.append(secretKey).append("&").append(timestamp);



        // Calculate MD5 hash
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(paramString.toString().getBytes(StandardCharsets.UTF_8));

            // Convert to hex and uppercase
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Verifies if the signature matches the expected value
     * @param data The data parameters
     * @param signature The signature to verify
     * @param timestamp The timestamp
     * @param secretKey The secret key
     * @return true if the signature is valid, false otherwise
     */
    public static boolean verifySignature(Map<String, Object> data, String signature, long timestamp, String secretKey) {
        String expectedSignature = generateSignature(data, secretKey, timestamp);
        // System.out.println(expectedSignature); // For debugging
        return secureCompare(signature, expectedSignature);
    }

    /**
     * Constant-time comparison to prevent timing attacks
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private static boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}