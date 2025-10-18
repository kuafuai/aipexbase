package com.kuafuai.error_report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.error_report.config.ErrorReportConfig;
import com.kuafuai.error_report.utils.SignatureUtils;
import com.kuafuai.system.entity.FrontendErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/error")
@Slf4j
public class ErrorReportController {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ErrorReportConfig errorReportConfig;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 进行前端报错信息的中转，上报到日志中心
     *
     * @param frontendErrorMessage 错误信息
     * @return
     */
    @PostMapping("/report")
    public BaseResponse errorReport(@RequestBody FrontendErrorMessage frontendErrorMessage) {
        final String reportUrl = errorReportConfig.getReportUrl();
        final String secretKey = errorReportConfig.getSecretKey();


        if (StringUtils.startsWithAny(reportUrl, "http://", "https://")) {

            HttpHeaders httpHeaders = addSecretHeader(frontendErrorMessage, secretKey);

            final HttpEntity<FrontendErrorMessage> httpEntity = new HttpEntity<>(frontendErrorMessage, httpHeaders);

            try {
                final ResponseEntity<Map> mapResponseEntity = restTemplate.postForEntity(reportUrl, httpEntity, Map.class);

                final HttpStatus statusCode = mapResponseEntity.getStatusCode();

                if (statusCode.is2xxSuccessful()) {
                    log.info("{},{}", "ErrorReportController", mapResponseEntity.getBody());
                    return ResultUtils.success(true);
                }
            }catch (HttpServerErrorException e){
                log.error("{},{}", "ErrorReportController", frontendErrorMessage,e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "前端报错信息上报失败" );
            }

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "前端报错信息上报失败");
        } else {
            return ResultUtils.success(true);
        }
    }

    /**
     * 添加鉴权
     *
     * @param frontendErrorMessage
     * @param secretKey
     * @return
     */
    private HttpHeaders addSecretHeader(Object frontendErrorMessage, String secretKey) {
        final HttpHeaders httpHeaders = new HttpHeaders();

        Map<String, Object> body = objectMapper.convertValue(frontendErrorMessage, Map.class);

        long timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        final String generateSignature = SignatureUtils.generateSignature(body, secretKey, timestamp);

        httpHeaders.add("X-Timestamp", String.valueOf(timestamp));
        httpHeaders.add("X-Signature", generateSignature);
        httpHeaders.add("X-App-Source", "baas-frontend-log");


        return httpHeaders;
    }
}
