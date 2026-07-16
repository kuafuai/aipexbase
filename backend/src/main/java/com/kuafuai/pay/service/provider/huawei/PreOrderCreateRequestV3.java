package com.kuafuai.pay.service.provider.huawei;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.petalpay.paymentservice.apiservice.client.model.PayerIn;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PreOrderCreateRequestV3 {
    @JsonProperty("spAppId")
    private String spAppId;
    @JsonProperty("spMercNo")
    private String spMercNo;

    @JsonProperty("subMercNo")
    private String subMercNo;


    @JsonProperty("mercOrderNo")
    private String mercOrderNo = null;
    @JsonProperty("tradeSummary")
    private String tradeSummary = null;
    @JsonProperty("totalAmount")
    private Long totalAmount = null;
    @JsonProperty("currency")
    private String currency = null;
    @JsonProperty("bizType")
    private String bizType = null;
    @JsonProperty("payer")
    private PayerIn payer = null;
    @JsonProperty("allocationType")
    private String allocationType = null;
    @JsonProperty("callbackUrl")
    private String callbackUrl = null;
    @JsonProperty("payload")
    private String payload = null;
    @JsonProperty("expireTime")
    private String expireTime = null;
    @JsonProperty("businessParams")
    private String businessParams = null;

}
