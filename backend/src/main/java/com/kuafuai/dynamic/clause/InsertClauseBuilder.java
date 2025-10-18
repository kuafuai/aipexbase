package com.kuafuai.dynamic.clause;

import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.system.entity.AppTableColumnInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsertClauseBuilder {

    private final TableContext ctx;

    public InsertClauseBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        Map<String, Object> cond = ctx.getConditions();
        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (AppTableColumnInfo c : ctx.getColumns()) {
            String key = c.getColumnName();
            if (!c.isPrimary() && cond.containsKey(key)) {
                keys.add("`" + key + "`");
                values.add("#{conditions." + key + "}");
            }
        }
        return "INSERT INTO " + ctx.qualifiedTable() + " (" + String.join(",", keys) + ") VALUES (" + String.join(",", values) + ")";
    }
}
