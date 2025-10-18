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
@TableName("app_table_info")
public class AppTableInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    private String appId;
    private String requirementId;

    private String tableName;
    private String physicalTableName;

    private String description;
}
