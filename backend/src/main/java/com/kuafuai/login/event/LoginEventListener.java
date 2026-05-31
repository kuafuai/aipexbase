package com.kuafuai.login.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.service.LoginColumnService;
import com.kuafuai.login.service.LoginService;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LoginEventListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private LoginService loginService;
    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private DynamicInfoCache dynamicInfoCache;
    @Autowired
    private LoginColumnService loginColumnService;


    @PostConstruct
    public void init() {
        //注册订阅者
        eventBus.register(this);
    }

    @Subscribe
    public void handleEvent(EventVo event) {
        log.info("===============Login event process:===={}", event);

        // 参数检查
        if (!validateEvent(event)) {
            return;
        }

        String database = event.getAppId();
        String tableName = event.getTableName();

        try {
            String rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(database, "app");
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);

            AppInfo appInfo = appInfoService.getAppInfoByAppId(database);

            if (appInfo == null) {
                log.info("No app info found for appId: {}", database);
                return;
            }

            if (!StringUtils.equalsIgnoreCase(tableName, appInfo.getAuthTable())) {
                log.info("app not need auth or table not same :{},{},{}", database, tableName, appInfo.getAuthTable());
                return;
            }

            String model = event.getModel();

            if (StringUtils.equalsIgnoreCase(model, "add")) {
                List<Login> logins = convert(database, tableName, event.getData());
                for (Login login : logins) {
                    loginService.save(database, login);
                }
            }
        } finally {
            log.info("==========login clear =========");
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    private boolean validateEvent(EventVo event) {
        if (event == null) {
            return false;
        }

        if (StringUtils.isEmpty(event.getAppId())) {
            return false;
        }

        if (StringUtils.isEmpty(event.getTableName())) {
            return false;
        }

        return true;
    }

    private List<Login> convert(String appId, String tableName, Object data) {
        if (!(data instanceof Map)) {
            log.warn("Login event data is not a Map, appId={}, table={}", appId, tableName);
            return Collections.emptyList();
        }
        Map<String, Object> mapData = (Map<String, Object>) data;
        List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, tableName);

        String encryptedPassword = SecurityUtils.encryptPassword(loginColumnService.findUserPassword(mapData, columnInfoList).map(Convert::toStr).orElse("123456"));
        String relevanceTable = StringUtils.dbStrToHumpLower(tableName);
        String relevanceId = String.valueOf(mapData.getOrDefault("_system_primary_id", "0"));

        List<Login> logins = new ArrayList<>();

        String phone = loginColumnService.findPhone(mapData, columnInfoList).map(Convert::toStr).orElse(null);
        String userName = loginColumnService.findUserName(mapData, columnInfoList).map(Convert::toStr).orElse(null);
        String email = loginColumnService.findEmail(mapData, columnInfoList).map(Convert::toStr).orElse(null);

        for (String identifier : new String[]{phone, userName, email}) {
            if (identifier == null) continue;
            Login login = new Login();
            login.setPhoneNumber(identifier);
            login.setUserName(identifier);

            login.setPassword(encryptedPassword);
            login.setRelevanceTable(relevanceTable);
            login.setRelevanceId(relevanceId);
            logins.add(login);
        }

        if (logins.isEmpty()) {
            log.warn("No identifier found, appId={}, table={}", appId, tableName);
        }
        return logins;
    }

}
