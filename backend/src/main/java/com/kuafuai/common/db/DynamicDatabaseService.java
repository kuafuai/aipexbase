package com.kuafuai.common.db;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.kuafuai.common.handler.DatabaseThreadLocal;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface DynamicDatabaseService<T> extends IService<T> {
    int DEFAULT_BATCH_SIZE = 1000;

    default boolean save(String database, T entity) {
        return withDatabase(database, () -> SqlHelper.retBool(this.getBaseMapper().insert(entity)));

    }

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean saveBatch(String database, Collection<T> entityList) {
        return withDatabase(database, () -> this.saveBatch(entityList));


//        return this.saveBatch(entityList, 1000);
    }

    boolean saveBatch(Collection<T> entityList, int batchSize);

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean saveOrUpdateBatch(String database, Collection<T> entityList) {
        return withDatabase(database, () -> this.saveOrUpdateBatch(entityList));

    }

    boolean saveOrUpdateBatch(String database, Collection<T> entityList, int batchSize);

    default boolean removeById(String database, Serializable id) {
        return withDatabase(database, () -> SqlHelper.retBool(this.getBaseMapper().deleteById(id)));
    }

    default boolean removeById(String database, Serializable id, boolean useFill) {
        throw new UnsupportedOperationException("不支持的方法!");
    }

    default boolean removeById(String database, T entity) {
        return withDatabase(database, () -> SqlHelper.retBool(this.getBaseMapper().deleteById(entity)));
    }

    default boolean removeByMap(String database, Map<String, Object> columnMap) {
        return withDatabase(database, () -> {
            Assert.notEmpty(columnMap, "error: columnMap must not be empty", new Object[0]);
            return SqlHelper.retBool(this.getBaseMapper().deleteByMap(columnMap));
        });

    }

    default boolean remove(String database, Wrapper<T> queryWrapper) {
        return withDatabase(database, () ->
                SqlHelper.retBool(this.getBaseMapper().delete(queryWrapper))
        );
    }

    default boolean removeByIds(String database, Collection<?> list) {
        return withDatabase(database, () -> CollectionUtils.isEmpty(list) ? false : SqlHelper.retBool(this.getBaseMapper().deleteBatchIds(list)));
    }

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean removeByIds(String database, Collection<?> list, boolean useFill) {
        return withDatabase(database, () -> {
            if (CollectionUtils.isEmpty(list)) {
                return false;
            } else {
                return useFill ? this.removeBatchByIds(database, list, true) : SqlHelper.retBool(this.getBaseMapper().deleteBatchIds(list));
            }
        });
    }

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean removeBatchByIds(String database, Collection<?> list) {
        return withDatabase(database, () -> this.removeBatchByIds(database, list, 1000));
    }

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean removeBatchByIds(String database, Collection<?> list, boolean useFill) {
        return withDatabase(database, () -> this.removeBatchByIds(database, list, 1000, useFill));
    }

    default boolean removeBatchByIds(String database, Collection<?> list, int batchSize) {
        throw new UnsupportedOperationException("不支持的方法!");
    }

    default boolean removeBatchByIds(String database, Collection<?> list, int batchSize, boolean useFill) {
        throw new UnsupportedOperationException("不支持的方法!");
    }

    default boolean updateById(String database, T entity) {
        return withDatabase(database, () -> SqlHelper.retBool(this.getBaseMapper().updateById(entity)));
    }

    default boolean update(String database, Wrapper<T> updateWrapper) {
        return withDatabase(database, () -> this.update(database, (T) null, updateWrapper));
    }

    default boolean update(String database, T entity, Wrapper<T> updateWrapper) {
        return withDatabase(database, () -> SqlHelper.retBool(this.getBaseMapper().update(entity, updateWrapper)));
    }

    @Transactional(
            rollbackFor = {Exception.class}
    )
    default boolean updateBatchById(String database, Collection<T> entityList) {
        return withDatabase(database, () -> this.updateBatchById(database, entityList, 1000));
    }

    boolean updateBatchById(String database, Collection<T> entityList, int batchSize);

    boolean saveOrUpdate(String database, T entity);

    default T getById(String database, Serializable id) {
        return withDatabase(database, () ->
                this.getBaseMapper().selectById(id)
        );
    }

    default List<T> listByIds(String database, Collection<? extends Serializable> idList) {
        return withDatabase(database, () -> this.getBaseMapper().selectBatchIds(idList));
    }

    default List<T> listByMap(String database, Map<String, Object> columnMap) {
        return withDatabase(database, () -> this.getBaseMapper().selectByMap(columnMap));
    }

    default T getOne(String database, Wrapper<T> queryWrapper) {
        return withDatabase(database, () -> this.getOne(database, queryWrapper, true));
    }

    T getOne(String database, Wrapper<T> queryWrapper, boolean throwEx);

    Map<String, Object> getMap(String database, Wrapper<T> queryWrapper);

    <V> V getObj(String database, Wrapper<T> queryWrapper, Function<? super Object, V> mapper);

    default long count(String database) {
        return withDatabase(database, () -> this.count(database, Wrappers.emptyWrapper()));
    }

    default long count(String database, Wrapper<T> queryWrapper) {
        return withDatabase(database, () -> SqlHelper.retCount(this.getBaseMapper().selectCount(queryWrapper)));
    }

    default List<T> list(String database, Wrapper<T> queryWrapper) {
        return withDatabase(database, () -> this.getBaseMapper().selectList(queryWrapper));
    }

    default List<T> list(String database) {
        return withDatabase(database, () -> this.list(database, Wrappers.emptyWrapper()));
    }

    default <E extends IPage<T>> E page(String database, E page, Wrapper<T> queryWrapper) {
        return withDatabase(database, () -> this.getBaseMapper().selectPage(page, queryWrapper));
    }

    default <E extends IPage<T>> E page(String database, E page) {
        return withDatabase(database, () -> this.page(database, page, Wrappers.emptyWrapper()));
    }

    default List<Map<String, Object>> listMaps(String database, Wrapper<T> queryWrapper) {
        return withDatabase(database, () -> this.getBaseMapper().selectMaps(queryWrapper));
    }

    default List<Map<String, Object>> listMaps(String database) {
        return this.listMaps(database, Wrappers.emptyWrapper());
    }

    default List<Object> listObjs(String database) {

        return this.listObjs(database, Function.identity());
    }

    default <V> List<V> listObjs(String database, Function<? super Object, V> mapper) {
        return this.listObjs(database, Wrappers.emptyWrapper(), mapper);
    }

    default List<Object> listObjs(String database, Wrapper<T> queryWrapper) {
        return this.listObjs(database, queryWrapper, Function.identity());
    }

    default <V> List<V> listObjs(String database, Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return withDatabase(database, () -> (List) this.getBaseMapper().selectObjs(queryWrapper).stream().filter(Objects::nonNull).map(mapper).collect(Collectors.toList()));

    }

    default <E extends IPage<Map<String, Object>>> E pageMaps(E page, Wrapper<T> queryWrapper) {
        return this.getBaseMapper().selectMapsPage(page, queryWrapper);
    }

    default <E extends IPage<Map<String, Object>>> E pageMaps(E page) {
        return this.pageMaps(page, Wrappers.emptyWrapper());
    }

    BaseMapper<T> getBaseMapper();

    Class<T> getEntityClass();

    default QueryChainWrapper<T> query(String database) {
        return withDatabase(database, () -> ChainWrappers.queryChain(this.getBaseMapper()));
    }

    default LambdaQueryChainWrapper<T> lambdaQuery(String database) {
        return withDatabase(database, () -> ChainWrappers.lambdaQueryChain(this.getBaseMapper()));
    }

    default KtQueryChainWrapper<T> ktQuery(String database) {
//        return ChainWrappers.ktQueryChain(this.getBaseMapper(), this.getEntityClass());
        return withDatabase(database, () -> ChainWrappers.ktQueryChain(this.getBaseMapper(), this.getEntityClass()));
    }

    default KtUpdateChainWrapper<T> ktUpdate(String database) {
        return withDatabase(database, () ->
                ChainWrappers.ktUpdateChain(this.getBaseMapper(), this.getEntityClass())
        );
    }

    default UpdateChainWrapper<T> update(String database) {
        return withDatabase(database, () -> ChainWrappers.updateChain(this.getBaseMapper()));
    }

    default LambdaUpdateChainWrapper<T> lambdaUpdate() {
        return ChainWrappers.lambdaUpdateChain(this.getBaseMapper());
    }

    default boolean saveOrUpdate(String database, T entity, Wrapper<T> updateWrapper) {
//        try {
//            DatabaseThreadLocal.setDatabase(database);
//            return this.update(database, entity, updateWrapper) || this.saveOrUpdate(database, entity);
//        }finally {
//            DatabaseThreadLocal.removeDatabase();
//        }
        return withDatabase(database, () -> this.update(database, entity, updateWrapper) || this.saveOrUpdate(database, entity));
    }


    default <R> R withDatabase(String database, Supplier<R> supplier) {
        try {
            DatabaseThreadLocal.setDatabase(database);
            return supplier.get();
        } finally {
            DatabaseThreadLocal.removeDatabase();
        }
    }
}
