package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.dynamic.clause.UpdateClauseBuilder;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.validation.ColumnValueChecker;

public class UpdateSqlBuilder {
    private final TableContext ctx;

    public UpdateSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        ColumnValueChecker.normalizeByUpdateValidate(ctx.getTable(), ctx.getColumns(), ctx.getConditions());

        return new UpdateClauseBuilder(ctx).build();
    }
}
