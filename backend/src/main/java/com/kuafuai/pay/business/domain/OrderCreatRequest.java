package com.kuafuai.pay.business.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreatRequest {
    private String orderNo;

    private Integer productId;

    private Integer userId;

    private String payChanel;

    private String opId;

    private Integer quality;

    private BigDecimal priceSingle;

    private BigDecimal totalPrice;

    private String remark;

    private String productSubject;




    /**
     * 物流方式
     */
    private String shippingMethod;

    /**
     * 收件人手机号
     */
    private String tel;

    /**
     * 收件人姓名
     */

    private String recipient;
    /**
     * 地址
     */

    private String shippingAddress;


    /**
     * 订单表
     */
    private String tableName;

    /**
     * 订单字段名
     */
    private String fieldName;

}
