package com.kuafuai.common.delay_task.handler;


import com.kuafuai.common.delay_task.constant.DelayTaskConstant;
import com.kuafuai.common.delay_task.domain.DBDelayTask;
import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.service.DelayedTasksService;

/**
 * db 延迟任务的处理类
 */
public class DBDelayedTaskHandlerWrapper extends AbstractDelayedTaskHandler<DelayTask> {

    private final AbstractDelayedTaskHandler abstractDelayedTaskHandler;

    private final DelayedTasksService delayedTasksService;

    public DBDelayedTaskHandlerWrapper(AbstractDelayedTaskHandler abstractDelayedTaskHandler,
                                       DelayedTasksService delayedTasksService) {
        this.abstractDelayedTaskHandler = abstractDelayedTaskHandler;
        this.delayedTasksService = delayedTasksService;
    }

    /**
     * 获取支持的参数类型（用于类型检查）
     */
    public Class<?> getSupportedType() {
        return abstractDelayedTaskHandler.getSupportedType();
    }


    /**
     * 抽象处理方法
     */
    public void handler(String database,DelayTask param) {
        if (param instanceof DBDelayTask) {
            DBDelayTask dbDelayTask = (DBDelayTask) param;
            try {
                delayedTasksService.updateTaskResult(database,dbDelayTask, DelayTaskConstant.RUNNING);
//              走原来的逻辑
                abstractDelayedTaskHandler.handler(database,dbDelayTask.getTaskPram());
//              更新任务结果为成功
                delayedTasksService.updateTaskResult(database,dbDelayTask, DelayTaskConstant.SUCCESS);
            } catch (Exception e) {
                delayedTasksService.updateTaskResult(database,dbDelayTask, DelayTaskConstant.FAIL);
                throw e;
            }
        } else {
            abstractDelayedTaskHandler.handler(database,param);
        }

    }


}
