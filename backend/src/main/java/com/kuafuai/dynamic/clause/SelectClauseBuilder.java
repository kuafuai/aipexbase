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
                selectBuilder.append(", ").append(alias).append(".").append(alias);
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
