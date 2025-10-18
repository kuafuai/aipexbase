package com.kuafuai.login.service;

import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.AppTableColumnInfo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
public class LoginColumnService {

    public final List<String> passwordKeys = Arrays.asList("passwd", "password", "pwd", "pass", "pword", "pws", "pswd");

    public final List<String> usernameKeys = Arrays.asList("username", "userName", "user_name", "usrname", "uname", "accountName", "acctName");
    public final List<String> phoneKeys = Arrays.asList(
            "phone", "phone_number", "phonenumber", "phoneNumber", "mobile",
            "mobile_number", "mobileNumber", "tel", "tel_number",
            "telephone", "telephone_number"
    );



    public Optional<Object> findUserIdentifier(Map<String, Object> mapData,
                                               List<AppTableColumnInfo> columnInfoList) {

        return findFirstMatchingColumn(mapData, columnInfoList, "phone")
                .map(Optional::of)
                .orElseGet(() -> findFirstMatchingKey(mapData, usernameKeys))
                .map(Optional::of)
                .orElseGet(() -> findFirstMatchingKey(mapData, phoneKeys))
                .map(Optional::of)
                .orElseGet(() -> findFirstMatchingColumn(mapData, columnInfoList, "email"));
    }


    public Optional<Object> findUserPassword(Map<String, Object> mapData,
                                             List<AppTableColumnInfo> columnInfoList) {

        return findFirstMatchingColumn(mapData, columnInfoList, "password")
                .map(Optional::of)
                .orElseGet(() -> findFirstMatchingKey(mapData, passwordKeys));
    }



    private Optional<Object> findFirstMatchingColumn(Map<String, Object> mapData,
                                                     List<AppTableColumnInfo> columnInfoList,
                                                     String dslType) {
        return columnInfoList.stream()
                .filter(p -> StringUtils.equalsIgnoreCase(p.getDslType(), dslType))
                .filter(p -> mapData.containsKey(p.getColumnName()))
                .findFirst()
                .map(p -> mapData.get(p.getColumnName()));
    }

    private Optional<Object> findFirstMatchingKey(Map<String, Object> mapData,
                                                  List<String> keys) {
        return keys.stream()
                .filter(mapData::containsKey)
                .findFirst()
                .map(mapData::get);
    }
}
