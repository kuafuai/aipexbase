package com.kuafuai.manage.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.manage.util.SystemTableUtil;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.mapper.DatabaseMapper;
import com.kuafuai.system.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

import static com.kuafuai.manage.service.ManageConstants.*;
import static com.kuafuai.manage.util.SystemTableUtil.*;

@Component
@Slf4j
public class ManageEventListener {

    private static final String MODEL_CREATE = "create";
    private static final String STATUS_ACTIVE = "active";

    @Autowired
    private EventBus eventBus;
    @Autowired
    private AppInfoService appInfoService;
    @Autowired
    private DatabaseMapper databaseMapper;

    @Autowired
    private ManageBusinessService manageBusinessService;

    @PostConstruct
    public void init() {
        //注册订阅者
        eventBus.register(this);
    }

    @Subscribe
    public void handleEvent(EventVo event) {
        log.info("Processing event for appId: {}, model: {}", event.getAppId(), event.getModel());

        if (!isValidCreateEvent(event)) {
            return;
        }

        String database = event.getAppId();
        AppInfo appInfo = appInfoService.getAppInfoByAppId(database);
        if (appInfo == null) {
            log.info("No app info found for appId: {}", database);
            return;
        }

        try {
            processDatabaseCreation(database, appInfo);
            log.info("Successfully processed database creation for appId: {}", database);
        } catch (Exception e) {
            log.error("Failed to process database creation for appId: {}", database, e);
        }
    }

    private boolean isValidCreateEvent(EventVo event) {
        return event != null && StringUtils.isNotBlank(event.getAppId()) && MODEL_CREATE.equalsIgnoreCase(event.getModel());
    }

    private void processDatabaseCreation(String appId, AppInfo appInfo) {
        databaseMapper.createDatabase(appId);
        updateAppStatus(appInfo);

        createSystemDatabaseTables(appId);
    }

    private void updateAppStatus(AppInfo appInfo) {
        appInfo.setStatus(STATUS_ACTIVE);
        appInfoService.updateById(appInfo);
    }

    private void createSystemDatabaseTables(String database) {
        List<TableVo> systemTables = createSystemTables(database);
        boolean success = manageBusinessService.createTables(database, systemTables);
        if (!success) {
            log.error("Failed to create system tables for database: {}", database);
        }
    }

    private List<TableVo> createSystemTables(String database) {
        return Arrays.asList(
                SystemTableUtil.gen_static_table(database, TABLE_STATIC_RESOURCES,
                        DESC_RESOURCES, "resource_id", static_resources_column),

                SystemTableUtil.gen_static_table(database, TABLE_LOGIN,
                        DESC_LOGIN, "login_id", login_columns),

                SystemTableUtil.gen_static_table(database, TABLE_SYSTEM_CONFIG,
                        DESC_SYSTEM_CONFIG, "id", kf_system_config)
        );
    }
}
