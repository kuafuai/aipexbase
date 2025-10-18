package com.kuafuai.common.delay_task.constant;

/**
 * 延迟任务的状态
 */
public class DelayTaskConstant {

    /**
     * 待执行
     */
    public static final String PENDING = "pending";

    /**
     * 正在运行
     */
    public static final String RUNNING = "running";

    /**
     * 任务成功
     */
    public static final String SUCCESS = "success";


    /**
     * 任务失败
     */
    public static final String FAIL = "fail";


    /**
     * 任务中断
     */
    @Deprecated
    public static final String shutdown = "shutdown";
}
