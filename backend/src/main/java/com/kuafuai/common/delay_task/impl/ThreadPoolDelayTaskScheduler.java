package com.kuafuai.common.delay_task.impl;

import com.kuafuai.common.delay_task.DelayTaskScheduler;
import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.domain.DelayedTasks;
import com.kuafuai.common.delay_task.domain.TypeDelayTaskWrapper;
import com.kuafuai.common.delay_task.handler.AbstractDelayedTaskHandler;
import com.kuafuai.common.delay_task.register.DelayTaskRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 单机版本的延迟任务
 */

@Component
public class ThreadPoolDelayTaskScheduler implements DelayTaskScheduler {


    private final ScheduledExecutorService scheduler;

    @Autowired
    private  DelayTaskRegister delayTaskRegister;

    public ThreadPoolDelayTaskScheduler(int poolSize) {
        this.scheduler = Executors.newScheduledThreadPool(poolSize);
    }

    public ThreadPoolDelayTaskScheduler() {
        int poolSize = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(poolSize+1);
    }

    @Override
    public void schedule(String database,DelayTask taskPram, long delay, TimeUnit unit) {
        AbstractDelayedTaskHandler delayedTaskHandler = delayTaskRegister.getHandler(taskPram.getClass());
        scheduler.schedule(new TypeDelayTaskWrapper(delayedTaskHandler,
                taskPram,database), delay, unit);
    }

    @Override
    public void applicationRun(String database, List<DelayedTasks> delayTaskList) {

    }


}
