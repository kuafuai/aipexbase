package com.kuafuai.system;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableInfo;
import com.kuafuai.system.entity.AppTableRelation;
import com.kuafuai.system.service.AppTableColumnInfoService;
import com.kuafuai.system.service.AppTableInfoService;
import com.kuafuai.system.service.AppTableRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class DynamicInfoCache {

    @Resource
    private Cache cache;

    @Autowired
    private AppTableInfoService appTableInfoService;
    @Autowired
    private AppTableColumnInfoService appTableColumnInfoService;
    @Autowired
    private AppTableRelationService appTableRelationService;

    private final static String APP_TABLE_KEY = "app_table_";
    private final static String APP_TABLE_COLUMN_KEY = "app_table_column_";
    private final static String APP_TABLE_RELATION_KEY = "app_table_relation_";

    public AppTableInfo getAppTableInfo(String appId, String table) {
        String key = APP_TABLE_KEY + appId + "_" + table;
        AppTableInfo appTableInfo = cache.getCacheObject(key);
        if (appTableInfo == null) {
            log.info("get table no cache {}:{}", appId, table);
            LambdaQueryWrapper<AppTableInfo> tableWrapper = new LambdaQueryWrapper<>();
            tableWrapper.eq(AppTableInfo::getAppId, appId);
            tableWrapper.eq(AppTableInfo::getTableName, table);

            appTableInfo = appTableInfoService.getOne(tableWrapper, false);
            if (appTableInfo != null) {
                cache.setCacheObject(key, appTableInfo);
            }
        } else {
            log.info("get table by cache {}:{}", appId, table);
        }
        return appTableInfo;
    }


    public List<AppTableColumnInfo> getAppTableColumnInfo(String appId, String table) {
        String key = APP_TABLE_COLUMN_KEY + appId + "_" + table;

        List<AppTableColumnInfo> columns = cache.getCacheList(key);
        if (columns == null) {
            log.info("get table column no cache {}:{}", appId, table);
            AppTableInfo tableInfo = getAppTableInfo(appId, table);
            if (tableInfo != null) {

                LambdaQueryWrapper<AppTableColumnInfo> columnWrapper = new LambdaQueryWrapper<>();
                columnWrapper.eq(AppTableColumnInfo::getTableId, tableInfo.getId());

                columns = appTableColumnInfoService.list(columnWrapper);

                if (columns != null && !columns.isEmpty()) {
                    cache.setCacheList(key, columns);
                }
            } else {
                log.error("===========get table error {}:{}", appId, table);
                columns = Lists.newArrayList();
            }
        } else {
            log.info("get table column by cache {}:{}", appId, table);
        }
        return columns;
    }


    public List<AppTableRelation> getTableRelations(String appId, String table) {
        String key = APP_TABLE_RELATION_KEY + appId + "_" + table;
        List<AppTableRelation> relations = cache.getCacheList(key);
        if (relations == null) {
            log.info("get table relations no cache {}:{}", appId, table);

            AppTableInfo tableInfo = getAppTableInfo(appId, table);

            LambdaQueryWrapper<AppTableRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(AppTableRelation::getAppId, appId);
            relationWrapper.eq(AppTableRelation::getTableId, tableInfo.getId());

            relations = appTableRelationService.list(relationWrapper);

            if (relations != null && !relations.isEmpty()) {
                cache.setCacheList(key, relations);
            }
        } else {
            log.info("get table relations by cache {}:{}", appId, table);
        }
        return relations;
    }


    public void clean(String appId) {
        cache.keys(APP_TABLE_KEY + appId).forEach(p -> {
            log.info("remove key=============={}", p);
            cache.deleteObject(p);
        });
        cache.keys(APP_TABLE_COLUMN_KEY + appId).forEach(p -> {
            log.info("remove key=============={}", p);
            cache.deleteObject(p);
        });
        cache.keys(APP_TABLE_RELATION_KEY + appId).forEach(p -> {
            log.info("remove key=============={}", p);
            cache.deleteObject(p);
        });
    }

}
