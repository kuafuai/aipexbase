package com.kuafuai.dynamic.clause;

import com.kuafuai.dynamic.context.TableContext;

public class DeleteClauseBuilder {

    private final TableContext ctx;

    public DeleteClauseBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String buildBase() {

        return "DELETE FROM " + ctx.qualifiedTable();
    }
}
