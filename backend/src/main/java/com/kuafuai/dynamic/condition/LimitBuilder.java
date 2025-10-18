package com.kuafuai.dynamic.condition;

import com.kuafuai.dynamic.context.Params;
import com.kuafuai.dynamic.context.TableContext;

import java.util.Map;

public class LimitBuilder {
    
    private final TableContext ctx;

    public LimitBuilder(TableContext ctx) {
        this.ctx = ctx;
    }

    public String build() {
        Map<String, Object> cond = ctx.getConditions();
        if (!cond.containsKey(Params.OFFSET))
            cond.put(Params.OFFSET, 0);
        if (!cond.containsKey(Params.LIMIT))
            cond.put(Params.LIMIT, 10);
        return "#{conditions." + Params.OFFSET + "}, #{conditions." + Params.LIMIT + "}";
    }

    public String buildOne() {
        return "1";
    }

}
