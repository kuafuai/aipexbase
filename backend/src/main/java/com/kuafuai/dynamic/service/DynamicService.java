package com.kuafuai.dynamic.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.event.EventService;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DynamicService {

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;

    @Autowired
    private EventService eventService;
    @Autowired
    private SystemBusinessService systemBusinessService;
    @Autowired
    private LoginBusinessService loginBusinessService;

    /**
     * 添加
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @Transactional
    public BaseResponse add(String database,
                            String table,
                            Map<String, Object> conditions) {
        AppInfo appInfo = Optional.ofNullable(DynamicAuthFilter.getAppInfo()).orElseGet(() -> systemBusinessService.getAppInfo(database));
        if (StringUtils.equalsAnyIgnoreCase(table, appInfo.getAuthTable())) {
            // 如果添加的是用户表，判断是否重复记录
            boolean flag = loginBusinessService.checkAuthRecordExist(database, table, conditions);
            if (flag) {
                return ResultUtils.error("数据已经存在，请检查数据。");
            }
        }
        long id = dynamicInterfaceService.add(database, table, conditions);
        conditions.put("_system_primary_id", id);
        eventService.publishEvent(EventVo.builder().appId(database).model("add").tableName(table).data(conditions).build());
        return ResultUtils.success(id);
    }


    @Transactional
    public BaseResponse addBatch(String database,
                                 String table,
                                 List<Map<String, Object>> conditions) {
        long count = dynamicInterfaceService.addBatch(database, table, conditions);
//        conditions.put("_system_primary_id", id);
//        eventService.publishEvent(EventVo.builder().appId(database).model("add").tableName(table).data(conditions).build());
        return ResultUtils.success(count);
    }

    @Transactional
    public BaseResponse deleteBatch(String database,
                                 String table,
                                 Map<String, Object> conditions) {
        long count = dynamicInterfaceService.deleteBatch(database, table, conditions);
        return ResultUtils.success(count);

    }

    /**
     * 跟新
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public BaseResponse update(String database,
                               String table,
                               Map<String, Object> conditions) {
        int value = dynamicInterfaceService.update(database, table, conditions);
        return ResultUtils.success(value);
    }

    /**
     * 删除
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public BaseResponse delete(String database,
                               String table,
                               Map<String, Object> conditions) {
        int value = dynamicInterfaceService.delete(database, table, conditions);
        return ResultUtils.success(value);
    }


    /**
     * 分页查询
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public BaseResponse page(String database,
                             String table,
                             Map<String, Object> conditions) {
        Page page = dynamicInterfaceService.page(database, table, conditions);
        return ResultUtils.success(page);
    }


    /**
     * get one
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public BaseResponse get(String database,
                            String table,
                            Map<String, Object> conditions) {
        Map<String, Object> data = dynamicInterfaceService.get(database, table, conditions);
        return data != null ? ResultUtils.success(data) : ResultUtils.error("数据不存在");
    }

    /**
     * list
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public BaseResponse list(String database,
                             String table,
                             Map<String, Object> conditions) {
        List<Map<String, Object>> list = dynamicInterfaceService.list(database, table, conditions);
        return ResultUtils.success(list);
    }


    /**
     * export
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    public void export(String database,
                       String table,
                       Map<String, Object> conditions) {
        dynamicInterfaceService.export(database, table, conditions);
    }
}
