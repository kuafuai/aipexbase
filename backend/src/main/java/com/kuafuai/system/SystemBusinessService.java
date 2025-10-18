package com.kuafuai.system;

import cn.hutool.http.HttpUtil;
import com.kuafuai.common.config.MessageConfig;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableInfo;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.AppTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Service
public class SystemBusinessService {

    @Autowired
    private DynamicInfoCache dynamicInfoCache;

    @Autowired
    private AppTableInfoService appTableInfoService;

    @Autowired
    private AppInfoService appInfoService;

    @Resource
    private MessageConfig messageConfig;

    public String getAppTablePrimaryKey(String appId, String tableName) {

        List<AppTableColumnInfo> columns = dynamicInfoCache.getAppTableColumnInfo(appId, tableName);

        return columns.stream()
                .filter(AppTableColumnInfo::isPrimary)
                .findFirst()
                .map(AppTableColumnInfo::getColumnName)
                .orElse("");
    }

    public String getAppTableNameById(Long tableId) {
        AppTableInfo tableInfo = appTableInfoService.getById(tableId);
        if (tableInfo != null) {
            return tableInfo.getTableName();
        } else {
            return "";
        }
    }

    public String getAppTableNameByIdAndAppId(Long tableId, String appId) {
        AppTableInfo tableInfo = appTableInfoService.getById(tableId);
        if (tableInfo != null && StringUtils.equalsIgnoreCase(appId, tableInfo.getAppId())) {
            return tableInfo.getTableName();
        } else {
            return null;
        }
    }

    public AppInfo getAppInfo(String appId) {
        return appInfoService.getAppInfoByAppId(appId);
    }


    public void sendMessage(String appId, String message) {
        try {
            String textMessage = "数据跟踪:\nAPP_ID: " + appId + "\n" + message;

            final HashMap<String, Object> body = new HashMap<>();
            body.put("msg_type", "text");
            final HashMap<String, Object> contentMap = new HashMap<>();
            contentMap.put("text", textMessage);

            body.put("content", contentMap);
            HttpUtil.post(messageConfig.getNotifyUrl(), JSON.toJSONString(body));
        } catch (Exception ignored) {

        }
    }
}
