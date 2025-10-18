package com.kuafuai.common.delay_task.impl;

import com.kuafuai.common.delay_task.DelayTaskScheduler;
import com.kuafuai.common.delay_task.domain.DBDelayTask;
import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.domain.DelayedTasks;
import com.kuafuai.common.delay_task.domain.TypeDelayTaskWrapper;
import com.kuafuai.common.delay_task.handler.AbstractDelayedTaskHandler;
import com.kuafuai.common.delay_task.handler.DBDelayedTaskHandlerWrapper;
import com.kuafuai.common.delay_task.register.DelayTaskRegister;
import com.kuafuai.common.delay_task.service.DelayedTasksService;
import com.kuafuai.common.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 操作结果将会放在数据库中
 */

@Component
@Primary // 默认使用这个
public class DbThreadPoolDelayTaskScheduler implements DelayTaskScheduler {


    private final ScheduledExecutorService scheduler;

    @Autowired
    private DelayTaskRegister delayTaskRegister;

    @Autowired
    private DelayedTasksService delayedTasksService;

    public DbThreadPoolDelayTaskScheduler(int poolSize) {
        this.scheduler = Executors.newScheduledThreadPool(poolSize);
    }

    public DbThreadPoolDelayTaskScheduler() {
        int poolSize = Runtime.getRuntime().availableProcessors();
        this.scheduler = Executors.newScheduledThreadPool(poolSize + 1);
    }

    @Override
    public void schedule(String database, DelayTask taskPram, long delay, TimeUnit unit) {
        final Class<? extends DelayTask> aClass = taskPram.getClass();
        final String taskName = aClass.getName();

        AbstractDelayedTaskHandler delayedTaskHandler = delayTaskRegister.getHandler(aClass);
        final Integer taskId = delayedTasksService.saveDelayTaskToDb(database, taskPram, delay, unit, taskName);

//      构建包装类
        final DBDelayTask dbDelayTask = new DBDelayTask(taskId, taskPram);
        final DBDelayedTaskHandlerWrapper dbAbstractDelayedTaskHandler = new DBDelayedTaskHandlerWrapper(delayedTaskHandler, delayedTasksService);

        scheduler.schedule(new TypeDelayTaskWrapper(dbAbstractDelayedTaskHandler,
                dbDelayTask, database), delay, unit);
    }

    @Override
    public void applicationRun(String database, List<DelayedTasks> delayTaskList) {
        for (DelayedTasks delayTask : delayTaskList) {
            final Integer taskId = delayTask.getId();
            final String taskData = delayTask.getTaskData();
            final String taskType = delayTask.getTaskType();
            final LocalDateTime executeTime = delayTask.getExecuteTime();


            long delay = 0;
            if (executeTime.isAfter(LocalDateTime.now())) {
//                如果是在当前时间之后，计算时间差多少分钟
                delay = executeTime.getSecond() - LocalDateTime.now().getSecond();

            }else {
//              将任务的执行时间均匀分布在应用启动后的5分钟之内
                delay = 1 + (long)(Math.random() * 300);
            }

            try {
                Class<? extends DelayTask> aClass = (Class<? extends DelayTask>) Class.forName(taskType);

                AbstractDelayedTaskHandler delayedTaskHandler = delayTaskRegister.getHandler(aClass);
                //      构建包装类
                final DBDelayTask dbDelayTask = new DBDelayTask(taskId, JSON.parseObject(taskData, aClass));
                final DBDelayedTaskHandlerWrapper dbAbstractDelayedTaskHandler = new DBDelayedTaskHandlerWrapper(delayedTaskHandler, delayedTasksService);

                scheduler.schedule(new TypeDelayTaskWrapper(dbAbstractDelayedTaskHandler,
                        dbDelayTask, database), delay, TimeUnit.SECONDS);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
