package com.kuafuai.manage.entity.vo;

import lombok.Data;

import java.util.Map;

@Data
public class DataVo {
    private String appId;
    private String tableName;
    private Map<String, Object> data;
}
