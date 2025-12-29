package com.kuafuai.manage.entity.vo;

import lombok.Data;

@Data
public class ApiTestResultVo {
    private Integer statusCode;
    private String message;
    private String responseBody;
    private Boolean success;
}