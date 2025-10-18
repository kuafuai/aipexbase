package com.kuafuai.common.dynamic_config.service;

import com.google.common.collect.Lists;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.dynamic_config.domain.SystemConfig;
import com.kuafuai.dynamic.service.DynamicService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ConfigManger {

    private static DynamicService dynamicService;

    private static final String CONFIG_TABLE = "system_config";


    private static Cache cache;

    private static SysConfigService sysConfigService;

    private static final String CACHE_KEY_CONFIG = "config:";

    public ConfigManger(@Autowired DynamicService dynamicService,
                        @Autowired Cache cache, @Autowired SysConfigService sysConfigService) {
        ConfigManger.dynamicService = dynamicService;
        ConfigManger.cache = cache;
        ConfigManger.sysConfigService = sysConfigService;
    }


    public static <T> T getConfig(String database, String propertyName, Class<T> clazz) {


        List<SystemConfig> cacheList = cache.getCacheList(CACHE_KEY_CONFIG + database);

        if (cacheList != null) {
            for (SystemConfig systemConfig : cacheList) {
                if (propertyName.equals(systemConfig.getName()) && ObjectUtils.isNotEmpty(systemConfig.getContent())) {
                    return convert(systemConfig.getContent(), clazz);
                }
            }
            return null;
        } else {
            cacheSystemConfig(database);
        }


//      执行了缓存逻辑，二次查询
        cacheList = cache.getCacheList(CACHE_KEY_CONFIG + database);

        if (cacheList != null) {
            for (SystemConfig systemConfig : cacheList) {
                if (propertyName.equals(systemConfig.getName()) && ObjectUtils.isNotEmpty(systemConfig.getContent())) {
                    return convert(systemConfig.getContent(), clazz);
                }
            }
        }

        return null;

    }

    private static void cacheSystemConfig(String database) {
        if (cache.hasKey(CACHE_KEY_CONFIG + database)) {
            return;
        }
        synchronized (database) {
            if (cache.hasKey(CACHE_KEY_CONFIG + database)) {
                return;
            }
//          缓存该所有的配置
            final List<SystemConfig> systemConfigs = sysConfigService.list(database);

            if (systemConfigs != null && !systemConfigs.isEmpty()) {
                cache.setCacheList(CACHE_KEY_CONFIG + database, systemConfigs);
                cache.expire(CACHE_KEY_CONFIG + database, 30, TimeUnit.SECONDS);
            }else {
//              说明config有问题，不存在，缓存3分钟的空列表
                cache.setCacheList(CACHE_KEY_CONFIG + database, Lists.newArrayList());
                cache.expire(CACHE_KEY_CONFIG + database, 30, TimeUnit.SECONDS);
            }
        }
    }


    private static <T> T convert(String content, Class<T> clazz) {
        if (content == null) {
            return null; // 或者抛出 IllegalArgumentException("Content cannot be null")
        }

        if (clazz == String.class) {
            return clazz.cast(content); // 直接返回原字符串
        } else if (clazz == Integer.class || clazz == int.class) {
            return clazz.cast(Integer.parseInt(content)); // 转成整数
        } else if (clazz == Long.class || clazz == long.class) {
            return clazz.cast(Long.parseLong(content)); // 转成长整数
        } else if (clazz == Double.class || clazz == double.class) {
            return clazz.cast(Double.parseDouble(content)); // 转成浮点数
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return clazz.cast(Boolean.parseBoolean(content)); // 转成布尔值
        }
        // 可以继续添加其他类型的支持，比如 Date、LocalDateTime 等

        throw new IllegalArgumentException("不支持的目标类型: " + clazz.getName());
    }


}
