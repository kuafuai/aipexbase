package com.kuafuai.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointCountVO {
    private String endpointGroup;
    private Long callCount;
    private Long errCount;
}
