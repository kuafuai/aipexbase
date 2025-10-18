package com.kuafuai.common.delay_task.domain;

import lombok.Data;

/**
 * db层面的任务
 */
@Data
public class DBDelayTask extends DelayTask {
    private Integer taskId;

    private DelayTask taskPram;


    public DBDelayTask(Integer taskId, DelayTask taskPram) {
        this.taskId = taskId;
        this.taskPram = taskPram;
    }
}
