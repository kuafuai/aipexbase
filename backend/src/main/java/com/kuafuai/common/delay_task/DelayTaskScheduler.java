package com.kuafuai.common.delay_task;


import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.domain.DelayedTasks;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface DelayTaskScheduler {


    /**
     * 提交一个延迟任务
     *
     * @param delay 延迟时间
     * @param unit  延迟时间单位
     */

    void schedule(String database, DelayTask taskPram, long delay, TimeUnit unit);


    /**
     * 应用启动时，干的事儿
     * @param database
     * @param delayTaskList
     */
    void applicationRun(String database, List<DelayedTasks> delayTaskList);

}
