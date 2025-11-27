package com.kuafuai.login.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.google.common.collect.Maps;
import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.RandomStringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class LoginBusinessService {

    @Autowired
    private LoginService loginService;

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;

    @Autowired
    private SystemBusinessService systemBusinessService;

    @Autowired
    private DynamicInfoCache dynamicInfoCache;
    @Autowired
    private LoginColumnService loginColumnService;

    /**
     * 检查用表记录是否重复
     */
    public boolean checkAuthRecordExist(String appId, String table, Map<String, Object> mapData) {
        List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, table);
        Optional<Object> userNameOrPhoneOptional = loginColumnService.findUserIdentifier(mapData, columnInfoList);

        if (userNameOrPhoneOptional.isPresent()) {
            String value = Convert.toStr(userNameOrPhoneOptional.get());
            Login login = getUserBySelectKey(appId, value, table);
            return login != null;
        } else {
            return false;
        }
    }

    public Login getUserBySelectKey(String appId, String key, String relevanceTable) {

        return getUserByField(appId, Login::getPhoneNumber, key, relevanceTable);
    }

    public Login getUserBySelectKey(String key, String relevanceTable) {

        return getUserByField(Login::getPhoneNumber, key, relevanceTable);
    }

    public Login getUserByOpenId(String openId, String relevanceTable) {
        return getUserByField(Login::getWxOpenId, openId, relevanceTable);
    }

    private <T> Login getUserByField(SFunction<Login, T> column, T value, String relevanceTable) {
        String database = GlobalAppIdFilter.getAppId();

        LambdaQueryWrapper<Login> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(column, value);

        if (isUserType()) {
            wrapper.eq(Login::getRelevanceTable, StringUtils.dbStrToHumpLower(resolveRelevanceTable(relevanceTable)));
        }

        return loginService.getOne(database, wrapper);
    }

    private <T> Login getUserByField(String database, SFunction<Login, T> column, T value, String relevanceTable) {

        LambdaQueryWrapper<Login> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(column, value);
        wrapper.eq(Login::getRelevanceTable, StringUtils.dbStrToHumpLower(resolveRelevanceTable(relevanceTable)));

        return loginService.getOne(database, wrapper);
    }

    public Object getCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        if (loginUser == null) {
            if (isUserType()) {
                return false;
            }
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "error.code.no_auth");
        }

        if (isUserType()) {
            String database = GlobalAppIdFilter.getAppId();
            String table = resolveRelevanceTable(loginUser.getRelevanceTable());

            String primaryKey = systemBusinessService.getAppTablePrimaryKey(database, table);
            Map<String, Object> params = Maps.newHashMap();
            params.put(primaryKey, loginUser.getRelevanceId());

            return dynamicInterfaceService.get(database, table, params);
        }

        return loginUser;
    }

    public Object getUserById(Object id, String appId, String tableName) {
        String primaryKey = systemBusinessService.getAppTablePrimaryKey(appId, tableName);
        Map<String, Object> params = Maps.newHashMap();
        params.put(primaryKey, id);

        return dynamicInterfaceService.get(appId, tableName, params);
    }

    public LoginUser getLoginUser(Login value, String relevanceTable) {
        String appId = GlobalAppIdFilter.getAppId();
        if (StringUtils.isEmpty(relevanceTable)) {
            relevanceTable = DynamicAuthFilter.getAppInfo().getAuthTable();
        }
        String relevanceId = value.getRelevanceId();
        return new LoginUser(appId, relevanceId, relevanceTable);
    }


    public Login createNewLoginByWechat(String openId) {
        String appId = GlobalAppIdFilter.getAppId();
        String authTable = DynamicAuthFilter.getAppInfo().getAuthTable();

        List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, authTable);
        Map<String, Object> valueMap = createAuthDataMap(columnInfoList);

        Long id = dynamicInterfaceService.add(appId, authTable, valueMap);

        Login login = createLogin(openId, authTable, id);
        loginService.save(appId, login);
        return login;
    }

    public Login createNewLoginByOauth2(String oauth2Id, String relevanceTable, String appId) {

        List<AppTableColumnInfo> columnInfoList = dynamicInfoCache.getAppTableColumnInfo(appId, relevanceTable);
        Map<String, Object> valueMap = createAuthDataMap(columnInfoList);

        Long id = dynamicInterfaceService.add(appId, relevanceTable, valueMap);

        Login login = createLogin(oauth2Id, relevanceTable, id);
        loginService.save(appId, login);
        return login;
    }



    private Login createLogin(String openId, String tableName, Long id) {
        Login login = new Login();
        login.setWxOpenId(openId);
        login.setUserName(openId);
        login.setPhoneNumber(openId);
        login.setPassword(SecurityUtils.encryptPassword("123456"));
        login.setRelevanceTable(StringUtils.dbStrToHumpLower(tableName));
        login.setRelevanceId(String.valueOf(id));
        return login;
    }


    private Map<String, Object> createAuthDataMap(List<AppTableColumnInfo> columnInfoList) {
        Map<String, Object> defaultMap = Maps.newHashMap();
        for (AppTableColumnInfo columnInfo : columnInfoList) {
            if (!columnInfo.isPrimary() && !columnInfo.isNullable()) {
                String colName = columnInfo.getColumnName();
                String dsl = columnInfo.getDslType();
                String type = columnInfo.getColumnType();

                if ("datetime".equalsIgnoreCase(dsl)) {
                    defaultMap.put(colName, DateUtils.getTime());
                } else if ("date".equalsIgnoreCase(dsl)) {
                    defaultMap.put(colName, DateUtils.getDate());
                } else if ("time".equalsIgnoreCase(dsl)) {
                    defaultMap.put(colName, DateUtils.getHhMmSs());
                } else if (Arrays.asList("number", "quote", "boolean", "decimal").contains(dsl)
                        || Arrays.asList("int", "float", "double").contains(type.toLowerCase())) {
                    defaultMap.put(colName, "1");
                } else {
                    defaultMap.put(colName, "码上飞_" + RandomStringUtils.generateRandomString(10));
                }
            }
        }
        return defaultMap;
    }


    private boolean isUserType() {
        return StringUtils.equalsIgnoreCase(GlobalAppIdFilter.getAppType(), "user");
    }


    private String resolveRelevanceTable(String relevanceTable) {
        if (StringUtils.isNotEmpty(relevanceTable)) {
            return relevanceTable;
        }
        return DynamicAuthFilter.getAppInfo().getAuthTable();
    }
}
