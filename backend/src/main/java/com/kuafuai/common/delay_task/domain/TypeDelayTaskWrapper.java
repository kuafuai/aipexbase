package com.kuafuai.common.delay_task.domain;


import com.kuafuai.common.delay_task.handler.AbstractDelayedTaskHandler;

/**
 * 包装类
 */
public class TypeDelayTaskWrapper implements Runnable {
    private final AbstractDelayedTaskHandler handler;
    private final DelayTask taskParam;

    private final String database;

    public TypeDelayTaskWrapper(AbstractDelayedTaskHandler handler, DelayTask taskParam,String database) {
        this.handler = handler;
        this.taskParam = taskParam;
        this.database = database;
    }

    @Override
    public void run() {
        handler.handler(database,taskParam);
    }
}
