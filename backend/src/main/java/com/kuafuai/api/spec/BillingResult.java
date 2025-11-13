package com.kuafuai.api.spec;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillingResult {
    private Integer quantity;
    private BigDecimal totalAmount;
}
