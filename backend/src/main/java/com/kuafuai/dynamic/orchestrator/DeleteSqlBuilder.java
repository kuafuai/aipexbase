package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.dynamic.clause.DeleteClauseBuilder;
import com.kuafuai.dynamic.condition.WhereBuilder;
import com.kuafuai.dynamic.context.TableContext;

public class DeleteSqlBuilder {
    private final TableContext ctx;

    public DeleteSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        String where = new WhereBuilder(ctx, false).build();
        if (where.isEmpty()) {
            throw new BusinessException(ctx.getDatabase() + ":" + ctx.getTable() + ":删除条件为空");
        }

        return new DeleteClauseBuilder(ctx).buildBase() + " WHERE " + where;
    }
}
