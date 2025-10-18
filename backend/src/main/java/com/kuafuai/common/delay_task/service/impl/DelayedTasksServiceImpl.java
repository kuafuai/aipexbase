package com.kuafuai.common.delay_task.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kuafuai.common.db.DynamicDataBaseServiceImpl;
import com.kuafuai.common.delay_task.config.DelayTaskConfig;
import com.kuafuai.common.delay_task.constant.DelayTaskConstant;
import com.kuafuai.common.delay_task.domain.DBDelayTask;
import com.kuafuai.common.delay_task.domain.DelayTask;
import com.kuafuai.common.delay_task.domain.DelayTaskAppInfo;
import com.kuafuai.common.delay_task.domain.DelayedTasks;
import com.kuafuai.common.delay_task.mapper.DelayedTasksMapper;
import com.kuafuai.common.delay_task.service.DelayTaskAppInfoService;
import com.kuafuai.common.delay_task.service.DelayedTasksService;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import com.kuafuai.dynamic.service.DynamicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

/**
 * @author www.macpe.cn
 * @description 针对表【delayed_tasks】的数据库操作Service实现
 * @createDate 2025-05-09 14:24:05
 */
@Service
public class DelayedTasksServiceImpl extends DynamicDataBaseServiceImpl<DelayedTasksMapper, DelayedTasks>
        implements DelayedTasksService {

    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private DelayTaskAppInfoService delayTaskAppInfoService;

    @Autowired
    private DelayTaskConfig delayTaskConfig;



    /**
     * 保存数据到数据库
     *
     * @param database
     * @param taskPram
     * @param delay
     * @param unit
     * @param taskName
     */

    @Override
    @Transactional
    public Integer saveDelayTaskToDb(String database, DelayTask taskPram, long delay, TimeUnit unit, String taskName) {
//      保存对应数据库的关系
        final DelayedTasks delayedTasks = new DelayedTasks();
        delayedTasks.setTaskType(taskName);
        delayedTasks.setTaskData(JSON.toJSONString(taskPram));
        delayedTasks.setStatus(DelayTaskConstant.PENDING);
        delayedTasks.setCreateTime(LocalDateTime.now());
        delayedTasks.setUpdateTime(LocalDateTime.now());
        delayedTasks.setExecuteTime(LocalDateTime.now().plus(delay, toChronoUnit(unit)));
//        save(delayedTasks);
        BaseResponse id = dynamicService.add(database, "delayed_tasks", delayedTasks.toMap());

        final Object data = id.getData();
        // 保存对应关系
        final DelayTaskAppInfo entity = new DelayTaskAppInfo();
        entity.setAppId(database);
        final Integer taskId = Integer.valueOf(String.valueOf(data));
        entity.setTaskId(taskId);
        entity.setTaskStatus(DelayTaskConstant.PENDING);
        entity.setServiceName(delayTaskConfig.getDelayTaskWorkerName());

        delayTaskAppInfoService.save(entity);
        return taskId;
    }


    @Override
    @Transactional
    public Boolean updateTaskResult(String database, DBDelayTask dbDelayTask, String status) {
//        //          更新任务结果
        final DelayedTasks entity = new DelayedTasks();
        entity.setId(dbDelayTask.getTaskId());
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
//        final boolean updateById = updateById(entity);

        BaseResponse delayedTasks = dynamicService.update(database, "delayed_tasks", entity.toMapNotNull());
        if (delayedTasks.getCode() != ErrorCode.SUCCESS.getCode()) {
            throw new BusinessException("任务更新失败");
        }


        final LambdaUpdateWrapper<DelayTaskAppInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DelayTaskAppInfo::getTaskId, dbDelayTask.getTaskId())
                .eq(DelayTaskAppInfo::getAppId, database)
                .set(DelayTaskAppInfo::getTaskStatus, status);

        return delayTaskAppInfoService.update(updateWrapper);
    }


    public static TemporalUnit toChronoUnit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit: " + timeUnit);
        }
    }

}




