package com.kuafuai.common.db;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.common.handler.DatabaseThreadLocal;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicDataBaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements DynamicDatabaseService<T> {

    @Override
    public boolean saveOrUpdateBatch(String database, Collection<T> entityList, int batchSize) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.saveOrUpdateBatch(entityList, batchSize);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }

    @Override
    public boolean updateBatchById(String database, Collection<T> entityList, int batchSize) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.updateBatchById(entityList, batchSize);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }

    @Override
    public boolean saveOrUpdate(String database, T entity) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.saveOrUpdate(entity);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }

    @Override
    public T getOne(String database, Wrapper<T> queryWrapper, boolean throwEx) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.getOne(queryWrapper, throwEx);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }

    @Override
    public Map<String, Object> getMap(String database, Wrapper<T> queryWrapper) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.getMap(queryWrapper);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }

    @Override
    public <V> V getObj(String database, Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return super.getObj(queryWrapper, mapper);
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }


    @Override
    public boolean save(T entity) {
        return super.save(entity);
    }

    @Override
    public boolean saveBatch(Collection<T> entityList) {
        return super.saveBatch(entityList);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList) {
        return super.saveOrUpdateBatch(entityList);
    }

    @Override
    public boolean removeById(T entity) {
        return super.removeById(entity);
    }

    @Override
    public boolean removeByMap(Map<String, Object> columnMap) {
        return super.removeByMap(columnMap);
    }

    @Override
    public boolean remove(Wrapper<T> queryWrapper) {
        return super.remove(queryWrapper);
    }

    @Override
    public boolean removeByIds(Collection<?> list, boolean useFill) {
        return super.removeByIds(list, useFill);
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list) {
        return super.removeBatchByIds(list);
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list, boolean useFill) {
        return super.removeBatchByIds(list, useFill);
    }

    @Override
    public boolean updateById(T entity) {
        return super.updateById(entity);
    }

    @Override
    public boolean update(Wrapper<T> updateWrapper) {
        return super.update(updateWrapper);
    }

    @Override
    public boolean update(T entity, Wrapper<T> updateWrapper) {
        return super.update(entity, updateWrapper);
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList) {
        return super.updateBatchById(entityList);
    }

    @Override
    public T getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public List<T> listByIds(Collection<? extends Serializable> idList) {
        return super.listByIds(idList);
    }

    @Override
    public List<T> listByMap(Map<String, Object> columnMap) {
        return super.listByMap(columnMap);
    }

    @Override
    public T getOne(Wrapper<T> queryWrapper) {
        return super.getOne(queryWrapper);
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public long count(Wrapper<T> queryWrapper) {
        return super.count(queryWrapper);
    }

    @Override
    public List<T> list(Wrapper<T> queryWrapper) {
        return super.list(queryWrapper);
    }

    @Override
    public List<T> list() {
        return super.list();
    }

    @Override
    public <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        return super.page(page, queryWrapper);
    }

    @Override
    public <E extends IPage<T>> E page(E page) {
        return super.page(page);
    }

    @Override
    public List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper) {
        return super.listMaps(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> listMaps() {
        return super.listMaps();
    }

    @Override
    public List<Object> listObjs() {
        return super.listObjs();
    }

    @Override
    public <V> List<V> listObjs(Function<? super Object, V> mapper) {
        return super.listObjs(mapper);
    }

    @Override
    public List<Object> listObjs(Wrapper<T> queryWrapper) {
        return super.listObjs(queryWrapper);
    }

    @Override
    public <V> List<V> listObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return super.listObjs(queryWrapper, mapper);
    }

    @Override
    public <E extends IPage<Map<String, Object>>> E pageMaps(E page, Wrapper<T> queryWrapper) {
        return super.pageMaps(page, queryWrapper);
    }

    @Override
    public <E extends IPage<Map<String, Object>>> E pageMaps(E page) {
        return super.pageMaps(page);
    }

    @Override
    public QueryChainWrapper<T> query() {
        return super.query();
    }

    @Override
    public LambdaQueryChainWrapper<T> lambdaQuery() {
        return super.lambdaQuery();
    }

    @Override
    public KtQueryChainWrapper<T> ktQuery() {
        return super.ktQuery();
    }

    @Override
    public KtUpdateChainWrapper<T> ktUpdate() {
        return super.ktUpdate();
    }

    @Override
    public UpdateChainWrapper<T> update() {
        return super.update();
    }

    @Override
    public LambdaUpdateChainWrapper<T> lambdaUpdate() {
        return super.lambdaUpdate();
    }

    @Override
    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper) {
        return super.saveOrUpdate(entity, updateWrapper);
    }
}
