package com.kuafuai.login.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        String database = event.getAppId();
        String tableName = event.getTableName();

        AppInfo appInfo = appInfoService.getAppInfoByAppId(database);

        if (appInfo == null) {
            log.info("No app info found for appId: {}", database);
            return;
        }

        if (!appInfo.getNeedAuth() || !StringUtils.equalsIgnoreCase(tableName, appInfo.getAuthTable())) {
            log.info("app not need auth or table not same :{},{},{}", database, tableName, appInfo.getAuthTable());
            return;
        }

        String model = event.getModel();

        if (StringUtils.equalsIgnoreCase(model, "add")) {
            Login login = convert(database, tableName, event.getData());
            if (login != null) {
                loginService.save(database, login);
            }
        }
    }

    private Login convert(String appId, String tableName, Object data) {

        Map<String, Object> mapData = (Map<String, Object>) data;
        List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, tableName);

        String password;
        Optional<Object> passwordOptional = loginColumnService.findUserPassword(mapData, columnInfoList);
        if (passwordOptional.isPresent()) {
            password = Convert.toStr(passwordOptional.get());
        } else {
            password = "123456";
        }

        Optional<Object> userNameOrPhoneOptional = loginColumnService.findUserIdentifier(mapData, columnInfoList);
        if (userNameOrPhoneOptional.isPresent()) {
            String value = Convert.toStr(userNameOrPhoneOptional.get());
            Login login = new Login();
            login.setUserName(value);
            login.setPhoneNumber(value);
            login.setPassword(SecurityUtils.encryptPassword(password));
            login.setRelevanceTable(StringUtils.dbStrToHumpLower(tableName));
            login.setRelevanceId(String.valueOf(mapData.getOrDefault("_system_primary_id", "0")));
            return login;
        } else {
            return null;
        }
    }
    
}
