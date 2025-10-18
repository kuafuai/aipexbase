package com.kuafuai.common.cache;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public interface Cache {
    /**
     * 缓存基本的对象，Integer、String、实体类等
     */
    <T> void setCacheObject(String key, T value);

    /**
     * 缓存基本的对象，Integer、String、实体类等
     */
    <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit);

    /**
     * 设置有效时间
     */
    boolean expire(String key, long timeout);

    /**
     * 设置有效时间
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取有效时间
     */
    long getExpire(String key);

    /**
     * 判断 key是否存在
     */
    Boolean hasKey(String key);

    /**
     * 获得缓存的基本对象。
     */
    <T> T getCacheObject(String key);

    /**
     * 删除单个对象
     */
    boolean deleteObject(String key);

    /**
     * 删除集合对象
     */
    boolean deleteObject(Collection<String> collection);

    /**
     * 缓存List数据
     */
    <T> long setCacheList(String key, List<T> dataList);

    <T> long setCacheList(String key, List<T> dataList, Integer timeout, TimeUnit timeUnit);

    /**
     * 获得缓存的list对象
     */
    <T> List<T> getCacheList(String key);

    /**
     * 缓存Set
     */
    <T> Set<T> setCacheSet(String key, Set<T> dataSet);

    /**
     * 获得缓存的set
     */
    <T> Set<T> getCacheSet(String key);

    /**
     * 缓存Map
     */
    <T> void setCacheMap(String key, Map<String, T> dataMap);

    /**
     * 获得缓存的Map
     */
    <T> Map<String, T> getCacheMap(String key);

    /**
     * 往Hash中存入数据
     */
    <T> void setCacheMapValue(String key, String hKey, T value);

    /**
     * 获取Hash中的数据
     */
    <T> T getCacheMapValue(String key, String hKey);

    /**
     * 获取多个Hash中的数据
     */
    <T> List<T> getMultiCacheMapValue(String key, Collection<Object> hKeys);

    /**
     * 删除Hash中的某条数据
     */
    boolean deleteCacheMapValue(String key, String hKey);

    /**
     * 获得缓存的基本对象列表
     */
    Collection<String> keys(String pattern);
}
