package com.kuafuai.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("app_table_column_info")
@EqualsAndHashCode
public class AppTableColumnInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;


    private String appId;
    private String requirementId;

    private Long tableId;

    private String columnName;
    private String columnType;

    private String dslType;

    private boolean isPrimary;
    private boolean isNullable;
    private boolean isShow;

    private String columnComment;
}
