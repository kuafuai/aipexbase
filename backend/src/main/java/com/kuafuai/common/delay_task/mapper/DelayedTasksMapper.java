package com.kuafuai.common.delay_task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.common.delay_task.domain.DelayedTasks;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component("commonDelayedTasksMapper")
/**
* @author www.macpe.cn
* @description 针对表【delayed_tasks】的数据库操作Mapper
* @createDate 2025-05-09 14:24:05
* @Entity generator.domain.DelayedTasks
*/
public interface DelayedTasksMapper extends BaseMapper<DelayedTasks> {

}




