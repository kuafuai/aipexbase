package com.kuafuai.dynamic.clause;

import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.List;

import static com.kuafuai.dynamic.helper.DynamicSelectStatement.getResourceColumns;

public class FromClauseBuilder {
    private final TableContext ctx;

    public FromClauseBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        String database = this.ctx.getDatabase();
        String table = this.ctx.getTable();
        List<AppTableColumnInfo> resourceCols = getResourceColumns(this.ctx.getColumns());
        StringBuilder fromBuilder = new StringBuilder(String.format(" `%s`.`%s` `%s` ", database, table, table));

        if (!resourceCols.isEmpty() && resourceCols.size() <= 2) {
            SystemBusinessService service = SpringUtils.getBean(SystemBusinessService.class);
            String pk = service.getAppTablePrimaryKey(database, table);
            for (AppTableColumnInfo col : resourceCols) {
                String alias = col.getColumnName();

                String joinSubquery = String.format(
                        "(SELECT * FROM `%s`.static_resources " +
                                "WHERE related_table_name='%s' " +
                                "AND relate_table_column_name='%s') AS %s",
                        database, table, alias, alias
                );

                fromBuilder.append("\n LEFT JOIN ")
                        .append(joinSubquery)
                        .append(" ON ")
                        .append(alias).append(".related_table_key = ")
                        .append("`").append(table).append("`.").append(pk).append(" ");
            }
        }
        return fromBuilder.toString();
    }
}
