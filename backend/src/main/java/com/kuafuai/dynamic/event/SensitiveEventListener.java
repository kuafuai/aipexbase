package com.kuafuai.dynamic.event;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SensitiveEventListener {

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        //注册订阅者
        eventBus.register(this);
    }

    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private SensitiveConfig sensitiveConfig;

    @Autowired
    private DynamicInfoCache dynamicInfoCache;

    @Resource
    private Cache cache;

    @Autowired
    private SystemBusinessService systemBusinessService;

    private static final String KEY = "sensitive:accesstoken:";

    private final SensitiveClient client = new SensitiveClient();

    @Subscribe
    public void handleEvent(EventVo event) {
        log.info("=====================sensitive===================");
        // 参数检查
        if (!validateEvent(event)) {
            return;
        }

        String database = event.getAppId();
        AppInfo appInfo = appInfoService.getAppInfoByAppId(database);

        if (appInfo == null) {
            log.info("No app info found for appId: {}", database);
            return;
        }

        String model = event.getModel();

        if (StringUtils.equalsAnyIgnoreCase(model, "add", "update")) {
            if (sensitiveConfig.isEnable()) {
                process(event);
            }
        }
    }

    private void process(EventVo event) {
        String appId = event.getAppId();
        String tableName = event.getTableName();
        String token = getAccessToken();

        if (StringUtils.isNotEmpty(token)) {

            //查询所有字段
            Map<String, Object> mapData = (Map<String, Object>) event.getData();
            List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, tableName);
            StringBuilder sb = new StringBuilder();
            //获取所有类型是text
            for (AppTableColumnInfo columnInfo : columnInfoList) {
                String dslType = columnInfo.getDslType();
                if (StringUtils.equalsAnyIgnoreCase(dslType, "text", "longtext", "string")) {
                    String columName = columnInfo.getColumnName();
                    if (mapData.containsKey(columName)) {
                        sb.append(mapData.get(columName)).append("\n");
                    }
                }
            }

            if (sb.length() > 0) {
                textCensorAndSendMsg(token, sb.toString(), event);
            }

        }

    }

    private String getAccessToken() {
        String accessToken = cache.getCacheObject(KEY);

        if (StringUtils.isNotEmpty(accessToken)) {
            return accessToken;
        }

        AccessTokenRequest request = AccessTokenRequest.builder()
                .clientId(sensitiveConfig.getAppKey())
                .clientSecret(sensitiveConfig.getSecretKey())
                .build();

        AccessTokenResponse response = client.getAccessToken(request);
        if (StringUtils.isNotEmpty(response.getAccessToken())) {
            accessToken = response.getAccessToken();
            int expires_in = response.getExpiresIn();
            // 写入缓存中
            cache.setCacheObject(KEY, accessToken, expires_in, TimeUnit.SECONDS);

            return accessToken;
        } else {
            return null;
        }
    }

    private void textCensorAndSendMsg(String token, String text, EventVo eventVo) {
        TextCensorRequest request = TextCensorRequest.builder()
                .accessToken(token)
                .text(text)
                .build();

        TextCensorResponse response = client.textCensor(request);
        if (response.getConclusionType() != null && (response.getConclusionType() == 2 || response.getConclusionType() == 3)) {
            //审核失败
            String msg = "百度审核接口失败,请及时查看。\n 内容：" + text + "\n";
            systemBusinessService.sendMessage(eventVo.getAppId(), msg);
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
}
