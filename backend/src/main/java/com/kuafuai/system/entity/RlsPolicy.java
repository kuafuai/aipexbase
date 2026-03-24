package com.kuafuai.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("app_rls_policy")
public class RlsPolicy {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String appId;
    private String tableName;
    private String policyName;

    private String operation;
    private String usingExpression;
    private String withCheckExpression;

    private Boolean enabled;
    private Integer priority;

    private String description;
}
