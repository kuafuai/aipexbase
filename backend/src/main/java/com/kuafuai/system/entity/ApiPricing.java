package com.kuafuai.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("api_pricing")
@EqualsAndHashCode
public class ApiPricing {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer marketId;
    private Integer pricingModel;
    private Double unitPrice;
}
