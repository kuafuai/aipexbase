package com.kuafuai.dynamic.clause;

import com.google.common.collect.Maps;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.condition.WhereBuilder;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.helper.ContextFactory;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateClauseBuilder {
    private final TableContext ctx;

    public UpdateClauseBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        Map<String, Object> cond = ctx.getConditions();

        Map<String, Object> whereCond = Maps.newHashMap();
        List<String> sets = new ArrayList<>();

        for (AppTableColumnInfo c : ctx.getColumns()) {
            String k = c.getColumnName();
            Object v = cond.get(k);

            if (v == null) continue;

            // ============ PRIMARY KEY ===============
            if (c.isPrimary()) {
                whereCond.put(k, v);
                continue;
            }

            // ============ WHERE (Map 类型) ============
            if (v instanceof Map) {
                Map<?, ?> mapVal = (Map<?, ?>) v;
                if (!mapVal.isEmpty()) {
                    whereCond.put(k, v);
                }
                continue;
            }

            // ============ SET (普通类型) ===============
            String str = String.valueOf(v);
            if (StringUtils.isNotNull(str)) {
                sets.add("`" + k + "` = #{conditions." + k + "}");
            }
        }

        if (sets.isEmpty()) {
            throw new BusinessException("dynamic.update.no_fields_to_update");
        }

        if (whereCond.isEmpty()) {
            throw new BusinessException("dynamic.update.no_where_conditions");
        }

        TableContext wc = ContextFactory.fromTableContext(ctx, whereCond);
        WhereBuilder whereBuilder = new WhereBuilder(wc, false);

        return "UPDATE " + ctx.qualifiedTable() +
                " SET " + String.join(", ", sets) +
                " WHERE " + whereBuilder.build();
    }
}
