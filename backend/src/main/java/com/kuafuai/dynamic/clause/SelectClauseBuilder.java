package com.kuafuai.dynamic.clause;

import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;
import java.util.stream.Collectors;

import static com.kuafuai.dynamic.helper.DynamicSelectStatement.*;

public class SelectClauseBuilder {

    private final TableContext ctx;

    public SelectClauseBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        return gen_select();
    }

    private String gen_select() {

        String baseSelectCols = buildSelectColumns(this.ctx.getTable(), this.ctx.getColumns());
        List<AppTableColumnInfo> resourceCols = getResourceColumns(this.ctx.getColumns());
        StringBuilder selectBuilder = new StringBuilder(baseSelectCols);

        if (!resourceCols.isEmpty() && resourceCols.size() <= 2) {
            for (AppTableColumnInfo col : resourceCols) {
                String alias = col.getColumnName();
                selectBuilder.append(", JSON_ARRAYAGG(JSON_OBJECT( ")
                        .append("'resource_id', ").append(alias).append(".resource_id, ")
                        .append("'resource_path', ").append(alias).append(".resource_path, ")
                        .append("'url', ").append(alias).append(".resource_path, ")
                        .append("'related_table_name', ").append(alias).append(".related_table_name, ")
                        .append("'relate_table_column_name', ").append(alias).append(".relate_table_column_name, ")
                        .append("'related_table_key', ").append(alias).append(".related_table_key")
                        .append(")) AS ").append(alias);
            }
        }
        return selectBuilder.toString();
    }

    private String buildSelectColumns(String table, List<AppTableColumnInfo> columns) {
        return columns.stream()
                .filter(c -> !isResource(c) && !isPassword(c))
                .map(c -> String.format("`%s`.%s", table, c.getColumnName()))
                .collect(Collectors.joining(", "));
    }
}
