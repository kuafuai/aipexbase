package com.kuafuai.common.delay_task.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.common.delay_task.DelayTaskScheduler;
import com.kuafuai.common.delay_task.constant.DelayTaskConstant;
import com.kuafuai.common.delay_task.domain.DelayTaskAppInfo;
import com.kuafuai.common.delay_task.domain.DelayedTasks;
import com.kuafuai.common.delay_task.service.DelayTaskAppInfoService;
import com.kuafuai.common.delay_task.service.DelayedTasksService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoadingPendingService implements ApplicationRunner {
    @Resource
    private DelayTaskScheduler delayTaskScheduler;
    @Resource
    private DelayTaskAppInfoService delayTaskAppInfoService;

    @Resource
    private DelayTaskConfig delayTaskConfig;

    @Resource
    private DelayedTasksService delayedTasksService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {


//        delayTaskScheduler.schedule();
            final LambdaQueryWrapper<DelayTaskAppInfo> queryWrapper
                    = new LambdaQueryWrapper<>();
            queryWrapper.eq(DelayTaskAppInfo::getServiceName,
                    delayTaskConfig.getDelayTaskWorkerName()).eq(DelayTaskAppInfo::getTaskStatus,
                    DelayTaskConstant.PENDING);
            final List<DelayTaskAppInfo> delayTaskAppInfos = delayTaskAppInfoService.list(queryWrapper);

            if (delayTaskAppInfos == null || delayTaskAppInfos.isEmpty()) {
                return;
            }
//      按照不同的库进行分组
            final Map<String, List<DelayTaskAppInfo>> mapByDatabase = delayTaskAppInfos.stream().collect(Collectors.groupingBy(DelayTaskAppInfo::getAppId));


            for (Map.Entry<String, List<DelayTaskAppInfo>> entry : mapByDatabase.entrySet()) {
                String database = entry.getKey();

                List<DelayTaskAppInfo> pendingTasks = entry.getValue();
                final Set<Integer> pendingTaskId = pendingTasks.stream().map(DelayTaskAppInfo::getTaskId)
                        .collect(Collectors.toSet());


                final LambdaQueryWrapper<DelayedTasks> tasksLambdaQueryWrapper = new LambdaQueryWrapper<>();
                tasksLambdaQueryWrapper.in(DelayedTasks::getId, pendingTaskId);
                tasksLambdaQueryWrapper.eq(DelayedTasks::getStatus, DelayTaskConstant.PENDING);
                List<DelayedTasks> delayedTasksList = delayedTasksService.list(database, tasksLambdaQueryWrapper);

                delayTaskScheduler.applicationRun(database, delayedTasksList);

            }
        } catch (Exception e) {
            log.error("加载延迟任务异常", e);
        }

    }
}
