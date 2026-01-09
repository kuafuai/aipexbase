package com.kuafuai.manage.entity.dto;

import com.kuafuai.system.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInfoCopyDTO {

    private AppInfo appInfo;

    private List<AppTableInfo> tables;

    private Map<Long, List<AppTableColumnInfo>> tableColumnMap;

    private List<AppTableRelation> relations;

    private List<AppRequirementSQL> sqls;
}
