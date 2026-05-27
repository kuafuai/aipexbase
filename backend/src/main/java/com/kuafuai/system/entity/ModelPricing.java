package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("model_pricing")
public class ModelPricing {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String modelName;

    // 单价单位: 元/1万tokens
    private BigDecimal promptUnitPrice;

    private BigDecimal completionUnitPrice;

    // cached tokens 折扣价
    private BigDecimal cacheUnitPrice;

    // 1-启用 0-禁用
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
