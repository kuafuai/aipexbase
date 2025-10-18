package com.kuafuai.dynamic.context;

import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class TableContext {

    private final String database;
    private final String table;
    private final Map<String, Object> conditions;
    private final List<AppTableColumnInfo> columns;

    public String qualifiedTable() {
        return String.format("`%s`.`%s`", database, table);
    }

    public String tableAlias() {
        return table;
    }
}
