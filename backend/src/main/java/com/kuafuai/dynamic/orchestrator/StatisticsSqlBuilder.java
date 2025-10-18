package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.condition.WhereBuilder;
import com.kuafuai.dynamic.context.TableContext;

import java.util.Map;

public class StatisticsSqlBuilder {
    private final TableContext ctx;

    public StatisticsSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        Map<String, Object> cond = ctx.getConditions();

        String field = String.valueOf(cond.getOrDefault("field", "*"));
        String group = String.valueOf(cond.getOrDefault("group_condition_list", ""));

        String where = new WhereBuilder(ctx, false).build();

        StringBuilder sb = new StringBuilder("SELECT ");
        if (StringUtils.isNotBlank(group)) {
            sb.append(group).append(" AS name, COUNT(").append(field).append(") AS value");
        } else {
            sb.append("COUNT(").append(field).append(") AS value");
        }
        sb.append(" FROM ").append(ctx.qualifiedTable());
        if (!where.isEmpty()) sb.append(" WHERE ").append(where);
        if (StringUtils.isNotBlank(group)) sb.append(" GROUP BY ").append(group);

        return sb.toString();
    }
}
