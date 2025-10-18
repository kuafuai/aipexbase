package com.kuafuai.dynamic.orchestrator;

import com.kuafuai.dynamic.clause.InsertClauseBuilder;
import com.kuafuai.dynamic.context.TableContext;
import com.kuafuai.dynamic.validation.ColumnValueChecker;
import com.kuafuai.dynamic.validation.RequiredColumnChecker;

public class InsertSqlBuilder {
    private final TableContext ctx;

    public InsertSqlBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {

        RequiredColumnChecker.check(ctx.getColumns(), ctx.getConditions());
        ColumnValueChecker.normalizeAndValidate(ctx.getColumns(), ctx.getConditions());

        return new InsertClauseBuilder(ctx).build();
    }
}
