package com.kuafuai.config.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.dto.RdsDTO;
import com.kuafuai.system.entity.APIKey;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseRouterAspect {

    public static String getOrAllocateRdsKey(String routeKey, String type) {
        boolean beanExist = SpringUtils.containsBean(RdsManager.class);
        if (!beanExist) {
            return "DEFAULT";
        }

        RdsManager rdsManager = SpringUtils.getBean(RdsManager.class);
        // 1. 去缓存读取
        String rdsKey = rdsManager.getRdsKeyByAppId(routeKey);
        String choose = "redis";
        // 2. 缓存不存在
        if (StringUtils.isEmpty(rdsKey)) {
            // 3. 去默认库读取一下数据看看数据存在不存在。
            if (recordExits(routeKey, type)) {
                //4. 在默认库里读取到数据，使用 默认库
                rdsKey = "DEFAULT";
                choose = "db";

                rdsManager.putRdsKeyByAppId(routeKey, rdsKey);
            } else {
                // 5. 根据权重获取一个rdsKey
                List<RdsDTO> rdsList = rdsManager.getRdsList();
                rdsKey = selectRdsKey(routeKey, rdsList);
                choose = "select";
                // 6. 设置到缓存里
                rdsManager.putRdsKeyByAppId(routeKey, rdsKey);
            }
        }
        log.info("========rdsKey: {}:{}:{}", routeKey, choose, rdsKey);
        return rdsKey;
    }

    public static boolean recordExits(String routeKey, String type) {
        if (StringUtils.equalsIgnoreCase("app", type)) {
            AppInfoService appInfoService = SpringUtils.getBean(AppInfoService.class);

            LambdaQueryWrapper<AppInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AppInfo::getAppId, routeKey);
            return appInfoService.count(queryWrapper) > 0;
        } else {
            ApplicationAPIKeysService applicationAPIKeysService = SpringUtils.getBean(ApplicationAPIKeysService.class);
            LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(APIKey::getKeyName, routeKey);

            return applicationAPIKeysService.count(queryWrapper) > 0;
        }
    }


    private static String selectRdsKey(String key, List<RdsDTO> rdsList) {

        if (rdsList == null || rdsList.isEmpty()) {
            throw new IllegalArgumentException("RDS 列表不能为空");
        }

        List<RdsDTO> validList = rdsList.stream()
                .filter(r -> r.getPriority() != null && r.getPriority() > 0)
                .collect(Collectors.toList());

        if (validList.isEmpty()) {
            throw new IllegalStateException("没有可用的 RDS priority");
        }

        double totalWeight = validList.stream()
                .mapToDouble(RdsDTO::getPriority)
                .sum();

        double hash = (hash(key) & 0x7fffffff) / (double) Integer.MAX_VALUE;
        double point = hash * totalWeight;

        double cursor = 0;
        for (RdsDTO rds : validList) {
            cursor += rds.getPriority();
            if (point < cursor) {
                return rds.getRdsKey();
            }
        }

        // 理论不会走到
        return validList.get(0).getRdsKey();
    }


    private static int hash(String key) {
        int h = 0;
        for (char c : key.toCharArray()) {
            h = 31 * h + c;
        }
        return h;
    }

}
