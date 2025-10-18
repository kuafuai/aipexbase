package com.kuafuai.common.delay_task.service;

import com.kuafuai.common.db.DynamicDatabaseService;
import com.kuafuai.common.delay_task.domain.DBDelayTask;
import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.domain.DelayedTasks;

import java.util.concurrent.TimeUnit;

/**
* @author www.macpe.cn
* @description 针对表【delayed_tasks】的数据库操作Service
* @createDate 2025-05-09 14:24:05
*/
public interface DelayedTasksService extends DynamicDatabaseService<DelayedTasks> {

    Integer saveDelayTaskToDb(String database, DelayTask taskPram, long delay, TimeUnit unit, String taskName);

    Boolean updateTaskResult(String database,DBDelayTask dbDelayTask, String status);
}
