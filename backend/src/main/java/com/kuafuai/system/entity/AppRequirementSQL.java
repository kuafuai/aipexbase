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
@TableName("app_requirement_sql")
public class AppRequirementSQL {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    private String appId;
    private String requirementId;


    private String content;

    private String backContent;
    private Integer version;

    private String dslContent;

    private String status;

}
