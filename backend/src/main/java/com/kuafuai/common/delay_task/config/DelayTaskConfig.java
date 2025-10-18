package com.kuafuai.common.delay_task.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Data
public class DelayTaskConfig {



    @Value("${delayTaskWorkerName}")
    private String delayTaskWorkerName="delay_task_service";


}
