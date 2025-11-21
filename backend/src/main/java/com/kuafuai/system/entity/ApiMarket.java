package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("api_market")
@EqualsAndHashCode
public class ApiMarket {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer providerId;

    @TableField(exist = false)
    private String providerName;
    private String category;

    private String name;
    private String description;

    private String url;
    private Integer method;
    private Integer protocol;

    private String authType;
    private String authConfig;

    private String token;

    private Integer bodyType;
    private String bodyTemplate;

    private String headers;

    private Integer dataType;
    private String dataPath;

    private String dataRow;
    private String varRow;

    private Integer status;
    private Date createdAt;
    
    // 添加isBilling字段，0：计费，1：不计费
    private Integer isBilling;

    @TableField(exist = false)
    private boolean owner;
    @TableField(exist = false)
    private Integer pricingModel;
    @TableField(exist = false)
    private Double unitPrice;
}