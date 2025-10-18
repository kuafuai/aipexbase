package com.kuafuai.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;


@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "local")
public class LocalCache implements Cache {

    @PostConstruct
    public void init() {
        System.out.println("=========================local");
    }

    //设置缓存对象
    //设置有效时间
    //判断key是否存在
    //获取基本对象
    //删除对象
    //List,Set,map,hash
    public Map<String, Object> cache = new ConcurrentHashMap<>();
    public Map<String, Long> expireTimeMap = new ConcurrentHashMap<>();


    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        cache.put(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */

    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        cache.put(key, value);
        if (timeout != null && timeUnit != null) {
            long time = System.currentTimeMillis() + timeUnit.toMillis(timeout);
            if (time > System.currentTimeMillis()) {
                expireTimeMap.put(key, time);
            } else {
                expireTimeMap.remove(key);
                cache.remove(key);
            }
        }
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        if (cache.containsKey(key)) {
            expireTimeMap.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
            return true;
        }
        return false;
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        if (expireTimeMap.containsKey(key)) {
            long expireTime = expireTimeMap.get(key);
            long currentTime = System.currentTimeMillis();
            if (expireTime > currentTime) {
                return expireTime - currentTime;
            } else {
                expireTimeMap.remove(key);
                cache.remove(key);
            }
        }
        return -1;
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return cache.containsKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        if (expireTimeMap.containsKey(key)) {
            if (getExpire(key) > 0) {
                return (T) cache.get(key);
            } else {
                return null;
            }
        } else {
            return (T) cache.get(key);
        }
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        cache.remove(key);
        expireTimeMap.remove(key);
        return true;
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection<String> collection) {
        collection.forEach(cache::remove);
        return true;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        List<T> list = new CopyOnWriteArrayList<>(dataList);
        cache.put(key, list);
        return list.size();
    }

    @Override
    public <T> long setCacheList(String key, List<T> dataList, Integer timeout, TimeUnit timeUnit) {
        List<T> list = new CopyOnWriteArrayList<>(dataList);
        cache.put(key, list);
        if (timeUnit != null && timeout != null) {
            long time = timeUnit.toMillis(timeout) + System.currentTimeMillis();
            if (System.currentTimeMillis() > time) {
                expireTimeMap.remove(key);
                cache.remove(key);
            } else {
                expireTimeMap.put(key, time);
            }
        }
        return list.size();
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        if (expireTimeMap.containsKey(key)) {
            if (getExpire(key) > 0) {
                return (List<T>) cache.get(key);
            } else {
                return null;
            }
        } else {
            return (List<T>) cache.get(key);
        }
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> Set<T> setCacheSet(final String key, final Set<T> dataSet) {
        Set<T> set = new CopyOnWriteArraySet<>(dataSet);
        cache.put(key, set);
        return set;
    }


    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return (Set<T>) cache.get(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        cache.put(key, new ConcurrentHashMap<>(dataMap));
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return (Map<String, T>) cache.get(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        Map<String, T> map = (Map<String, T>) cache.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        map.put(hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        Map<String, T> map = (Map<String, T>) cache.get(key);
        return map != null ? map.get(hKey) : null;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        List<T> result = new CopyOnWriteArrayList<>();
        Map<String, T> map = (Map<String, T>) cache.get(key);
        if (map != null) {
            for (Object hKey : hKeys) {
                T value = map.get(hKey);
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        Map<String, ?> map = (Map<String, ?>) cache.get(key);
        return map != null && map.remove(hKey) != null;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        List<String> result = new CopyOnWriteArrayList<>();
        for (String key : cache.keySet()) {
            if (key.startsWith(pattern)) {
                result.add(key);
            }
        }
        return result;
    }
}
