package com.kuafuai.manage.entity.vo;

import com.kuafuai.common.domin.PageRequest;
import lombok.Data;

@Data
public class ApiMarketVo extends PageRequest {
    private Integer id;

    private String name;
    private String description;
    private String category;
    private Integer status;

    private String url;
    private Integer protocol;
    private Integer method;
    private String headers;


    private String authType;
    private String authConfig;
    private String token;

    private Integer bodyType;
    private String bodyTemplate;

    private Integer dataType;
    private String dataPath;

    private String dataRow;
    private String varRow;

    private Integer pricingModel;
    private Double unitPrice;
    
    // 添加 isBilling 字段
    private Integer isBilling;
}