package com.kuafuai.manage.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnVo {
    private Long id;
    private String appId;
    private Long tableId;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String columnType;
    private String dslType;
    private String defaultValue;
    private Boolean isPrimary;
    private Boolean isNullable;
    private Boolean isShow;
    private Long referenceTableId;
    private String referenceTableName;
    private String showName;
}
