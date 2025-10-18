package com.kuafuai.pay.domain;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class OrderOperateRequest {

    /**
     * 操作名称
     */
    @NotBlank(message = "操作名称不能为空")
    private String operateName;
    /**
     * 需要的参数
     */
    private Map object;

}
