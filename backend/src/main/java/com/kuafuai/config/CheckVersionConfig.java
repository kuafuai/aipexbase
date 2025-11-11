package com.kuafuai.config;

import com.google.common.collect.Maps;
import com.kuafuai.common.config.AppConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * 用于检测是有有新版本
 */
@Component
@Slf4j
public class CheckVersionConfig implements ApplicationRunner {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private VersionInfoHolder versionInfoHolder;
    private final static String VERSION_CHECK_URL = "https://www.codeflying.net/hw_agent/aiPexBase/checks";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {

            Map<String, Object> body = Maps.newHashMap();
            body.put("version", AppConfig.getVersion());
            body.put("channel", AppConfig.getChannel());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<VersionResponse> response = restTemplate.postForEntity(VERSION_CHECK_URL, entity, VersionResponse.class);

            VersionResponse result = Objects.requireNonNull(response.getBody());

            versionInfoHolder.setVersion(AppConfig.getVersion());
            versionInfoHolder.setLatestVersion(result.getVersion());
            versionInfoHolder.setHasUpdate(result.isHasUpdate());
            versionInfoHolder.setNotes(result.getNotes());
            versionInfoHolder.setMessage(result.getMessage());

            log.info("版本检测完成: 当前版本={}, 最新版本={}, hasUpdate={}",
                    versionInfoHolder.getVersion(),
                    versionInfoHolder.getLatestVersion(),
                    versionInfoHolder.isHasUpdate());
        } catch (Exception ignored) {

        }
    }

    @Data
    public static class VersionResponse {
        private String notes;
        private boolean hasUpdate;
        private String message;
        private String version;
    }
}
