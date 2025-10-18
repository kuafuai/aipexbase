package com.kuafuai.manage.entity.vo;

import com.kuafuai.common.domin.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableVo extends PageRequest {
    private Long id;
    private String appId;
    private String tableName;
    private String description;

    private List<ColumnVo> columns;
}
