package com.kuafuai.dynamic.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.common.util.UUID;
import com.kuafuai.dynamic.mapper.DynamicMapper;
import com.kuafuai.dynamic.mapper.DynamicStatisticsMapper;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableRelation;
import jodd.net.URLCoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynamicInterfaceService {

    @Autowired
    private DynamicInfoCache dynamicInfoCache;
    @Autowired
    private SystemBusinessService systemBusinessService;

    @Resource
    private DynamicMapper dynamicMapper;

    @Resource
    private DynamicStatisticsMapper dynamicStatisticsMapper;

    private final static String CURRENT = "current";
    private final static String PAGE_SIZE = "pageSize";
    private final static String OFFSET = "offset";
    private final static String LIMIT = "limit";

    private final Type config_value_type = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final Type static_resource_type = new TypeToken<List<Map<String, Object>>>() {
    }.getType();


    @Transactional
    public Long add(String database,
                    String table,
                    Map<String, Object> conditions) {
        // 提取资源字段
        Map<String, Object> resourceMapData = extractResourceColumns(database, table, conditions);

        dynamicMapper.insert(database, table, conditions);

        long id = dynamicMapper.getLastId();

        // 保存资源
        saveResources(database, table, id, resourceMapData);

        return id;
    }

    @Transactional
    public int update(String database,
                      String table,
                      Map<String, Object> conditions) {

        String primaryKey = systemBusinessService.getAppTablePrimaryKey(database, table);
        if (!conditions.containsKey(primaryKey)) {
            return dynamicMapper.update(database, table, conditions);
        }

        Map<String, Object> resourceMapData = extractResourceColumns(database, table, conditions);
        Object primaryKeyValue = conditions.get(primaryKey);

        // 删除历史资源
        if (!resourceMapData.isEmpty()) {
            Map<String, Object> deleteParams = Maps.newHashMap();
            deleteParams.put("related_table_name", table);
            deleteParams.put("related_table_key", primaryKeyValue);
            dynamicMapper.delete(database, "static_resources", deleteParams);
        }

        int value = dynamicMapper.update(database, table, conditions);

        // 保存新资源
        saveResources(database, table, primaryKeyValue, resourceMapData);

        return value;
    }

    private Map<String, Object> extractResourceColumns(String database, String table, Map<String, Object> conditions) {
        //所有字段
        List<AppTableColumnInfo> columnInfos = dynamicInfoCache.getAppTableColumnInfo(database, table);
        List<AppTableColumnInfo> resourceColumns = columnInfos.stream()
                .filter(p -> StringUtils.equalsAnyIgnoreCase(p.getDslType(), "image", "images", "file", "files", "video", "videos"))
                .collect(Collectors.toList());

        Map<String, Object> resourceMapData = Maps.newHashMap();
        if (!resourceColumns.isEmpty()) {
            for (AppTableColumnInfo appTableColumnInfo : resourceColumns) {
                String columnName = appTableColumnInfo.getColumnName();
                if (Objects.nonNull(conditions.get(columnName))) {
                    resourceMapData.put(columnName, conditions.get(columnName));
                }
                conditions.put(columnName, columnName);
            }
        }
        return resourceMapData;
    }

    private void saveResources(String database, String table, Object keyValue, Map<String, Object> resourceMapData) {
        if (resourceMapData.isEmpty()) return;
        for (Map.Entry<String, Object> entry : resourceMapData.entrySet()) {
            String columnName = entry.getKey();
            Object resourceValueObj = entry.getValue();
            if (resourceValueObj instanceof String) {
                saveResource(database, table, columnName, keyValue, (String) resourceValueObj);
            } else if (resourceValueObj instanceof List) {
                for (Object item : (List<?>) resourceValueObj) {
                    if (item instanceof String) {
                        saveResource(database, table, columnName, keyValue, (String) item);
                    } else if (item instanceof Map) {
                        Object url = ((Map<?, ?>) item).get("url");
                        if (url instanceof String) {
                            saveResource(database, table, columnName, keyValue, (String) url);
                        }
                    }
                }
            }
        }
    }

    private void saveResource(String database, String table, String key, Object id, String url) {
        if (StringUtils.startsWithAny(url, "https://", "http://")) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("related_table_name", table);
            params.put("relate_table_column_name", key);
            params.put("related_table_key", id);
            params.put("resource_path", url);
            dynamicMapper.insert(database, "static_resources", params);
        }
    }


    public int delete(String database,
                      String table,
                      Map<String, Object> conditions) {
        return dynamicMapper.delete(database, table, conditions);
    }

    public Map<String, Object> get(String database,
                                   String table,
                                   Map<String, Object> conditions) {
        Map<String, Object> data = dynamicMapper.getOne(database, table, conditions);
        if (data != null) {
            List<Map<String, Object>> list = Lists.newArrayList(data);
            process_relation_table(database, table, list);

            return list.get(0);
        } else {
            return null;
        }
    }

    public Page page(String database,
                     String table,
                     Map<String, Object> conditions) {
        long current = parseLongOrDefault(conditions, CURRENT, 1);
        long pageSize = parseLongOrDefault(conditions, PAGE_SIZE, 10);

        current = Math.max(current, 1);
        pageSize = Math.max(pageSize, 1);

        long offset = (current - 1) * pageSize;
        conditions.put(OFFSET, offset);
        conditions.put(LIMIT, pageSize);

        Page page = new Page(current, pageSize);
        page.setTotal(dynamicMapper.count(database, table, conditions));
        List<Map<String, Object>> list = dynamicMapper.page(database, table, conditions);

        process_relation_table(database, table, list);

        page.setRecords(list);

        return page;
    }

    public List<Map<String, Object>> list(String database,
                                          String table,
                                          Map<String, Object> conditions) {
        List<Map<String, Object>> list = dynamicMapper.list(database, table, conditions);
        if (conditions.containsKey("select_show_name")) {
            String primaryKey = systemBusinessService.getAppTablePrimaryKey(database, table);
            String showName = Convert.toStr(conditions.get("select_show_name"));

            log.info("select list {},{},{}", database, table, primaryKey);

            return list.stream()
                    .filter(m -> m.get(primaryKey) != null && m.get(showName) != null)
                    .map(m -> ImmutableMap.<String, Object>builder()
                            .put("value", m.get(primaryKey))
                            .put("label", m.get(showName))
                            .build())
                    .collect(Collectors.toList());
        } else {
            process_relation_table(database, table, list);
            return list;
        }
    }

    public void export(String database,
                       String table,
                       Map<String, Object> conditions) {
        List<Map<String, Object>> data = list(database, table, conditions);

        try {
            if (data == null || data.isEmpty()) {
                throw new BusinessException("dynamic.list.no_data_export");
            }

            // 1. 动态生成表头
            Map<String, Object> firstRow = data.get(0);
            List<String> fieldKeys = new ArrayList<>(firstRow.keySet()); // 保证顺序
            List<List<String>> head = new ArrayList<>();
            for (String key : fieldKeys) {
                head.add(Collections.singletonList(key));
            }

            // 2. 组装数据
            List<List<Object>> rows = new ArrayList<>();
            for (Map<String, Object> row : data) {
                List<Object> rowData = new ArrayList<>();
                for (String key : fieldKeys) {
                    Object value = row.get(key);
                    rowData.add(value == null ? "" : value.toString());
                }
                rows.add(rowData);
            }

            HttpServletResponse response = ServletUtils.getResponse();
            String uuid = UUID.randomUUID().toString();
            String fileName = String.format("%s_%s.xlsx", table, uuid);

            // 2. 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLCoder.encode(fileName, "UTF-8"));

            // 表头样式
            WriteCellStyle headCellStyle = new WriteCellStyle();
            headCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); // 背景色
            WriteFont headFont = new WriteFont();
            headFont.setFontHeightInPoints((short) 12); // 字体大小
            headFont.setBold(true); // 加粗
            headCellStyle.setWriteFont(headFont);

            // 内容样式
            WriteCellStyle contentCellStyle = new WriteCellStyle();
            contentCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER); // 居中
            WriteFont contentFont = new WriteFont();
            contentFont.setFontHeightInPoints((short) 11); // 内容字体
            contentCellStyle.setWriteFont(contentFont);

            // 策略
            HorizontalCellStyleStrategy styleStrategy =
                    new HorizontalCellStyleStrategy(headCellStyle, contentCellStyle);

            // 5. 导出
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // 自动列宽
                    .sheet("Sheet1")
                    .doWrite(rows);
        } catch (Exception e) {
            throw new BusinessException("dynamic.list.export.error");
        }
    }


    public List<Map<String, Object>> count(String database,
                                           String table,
                                           Map<String, Object> conditions) {

        return dynamicStatisticsMapper.count(database, table, conditions);
    }


    private long parseLongOrDefault(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 处理关联表数据
     * 主要功能：
     * 1. 处理资源类型字段（image、file、video 等）
     * 2. 处理 quote 类型字段（关联其他表的数据）
     * 3. 对敏感字段进行脱敏处理
     * <p>
     * 性能优化：
     * - 资源字段：根据字段数量选择不同的查询策略
     * - quote 字段：使用批量查询，避免 N+1 问题
     *
     * @param database 数据库名
     * @param table    表名
     * @param list     待处理的数据列表
     */
    private void process_relation_table(String database,
                                        String table,
                                        List<Map<String, Object>> list) {
        //所有字段
        List<AppTableColumnInfo> columnInfos = dynamicInfoCache.getAppTableColumnInfo(database, table);
        // 表对应的所有关系
        List<AppTableRelation> relations = dynamicInfoCache.getTableRelations(database, table);

        //处理是资源的
        process_resource_column(database, table, columnInfos, list);

        //处理是quote类型的
        List<AppTableColumnInfo> quoteColumns = columnInfos.stream()
                .filter(p -> StringUtils.equalsIgnoreCase(p.getDslType(), "quote"))
                .collect(Collectors.toList());

        if (!quoteColumns.isEmpty()) {
            log.debug("Processing {} quote columns for table: {}, data count: {}", quoteColumns.size(), table, list.size());

            for (AppTableColumnInfo appTableColumnInfo : quoteColumns) {
                Long columnId = appTableColumnInfo.getId();

                Optional<AppTableRelation> relationOptional = relations.stream()
                        .filter(p -> columnId.equals(p.getTableColumnId()))
                        .findFirst();

                if (relationOptional.isPresent()) {
                    process_quote_column(database, appTableColumnInfo, relationOptional.get(), list);
                } else {
                    log.warn("Quote column {} in table {} has no relation defined", appTableColumnInfo.getColumnName(), table);
                }
            }
        }
        if (StringUtils.equalsAnyIgnoreCase(table, "system_config") || StringUtils.equalsAnyIgnoreCase(table, "kf_system_config")) {
            log.info("============  mask_sensitive_fields  ======== : {},{}", database, table);
            mask_sensitive_fields(list, database);
        }
    }

    /**
     * 处理资源字段
     *
     * @param columnInfos
     * @param list
     */
    private void process_resource_column(String database,
                                         String table,
                                         List<AppTableColumnInfo> columnInfos,
                                         List<Map<String, Object>> list) {

        List<AppTableColumnInfo> resourceColumns = columnInfos.stream()
                .filter(p -> StringUtils.equalsAnyIgnoreCase(p.getDslType(), "image", "images", "file", "files", "video", "videos"))
                .collect(Collectors.toList());

        if (resourceColumns.isEmpty()) {
            return;
        }
        if (resourceColumns.size() <= 2) {
            for (AppTableColumnInfo appTableColumnInfo : resourceColumns) {
                String resourceColumn = appTableColumnInfo.getColumnName();
                for (Map<String, Object> map : list) {
                    String strResourceValue = Convert.toStr(map.get(resourceColumn));
                    if (StringUtils.isNotEmpty(strResourceValue)) {

                        List<Map<String, Object>> resources = JSON.parseObject(strResourceValue, static_resource_type);
                        resources.removeIf(r -> r.values().stream().allMatch(v -> v == null || "".equals(v)));

                        resources = resources.stream()
                                .sorted(Comparator.comparing(resourcesMap ->
                                        ((Number) resourcesMap.get("resource_id")).intValue()))
                                .collect(Collectors.toList());

                        resources.forEach(this::normalizeResource);
                        map.put(resourceColumn, resources);

                    } else {
                        map.put(resourceColumn, Collections.emptyList());
                    }
                }
            }
        } else {
            String primaryKey = systemBusinessService.getAppTablePrimaryKey(database, table);
            for (AppTableColumnInfo appTableColumnInfo : resourceColumns) {
                String resourceColumnName = appTableColumnInfo.getColumnName();
                for (Map<String, Object> data : list) {
                    String primaryKeyValue = Convert.toStr(data.get(primaryKey));

                    Map<String, Object> params = Maps.newHashMap();
                    params.put("related_table_name", table);
                    params.put("relate_table_column_name", resourceColumnName);
                    params.put("related_table_key", primaryKeyValue);

                    List<Map<String, Object>> resourceList = dynamicMapper.list(database, "static_resources", params);
                    if (resourceList != null && !resourceList.isEmpty()) {
                        resourceList.forEach(this::normalizeResource);
                        resourceList = resourceList.stream()
                                .sorted(Comparator.comparing(resourcesMap ->
                                        ((Number) resourcesMap.get("resource_id")).intValue()))
                                .collect(Collectors.toList());
                    }
                    data.put(resourceColumnName, resourceList);
                }
            }
        }
    }


    /**
     * 处理 quote 类型字段（批量查询优化版本）
     * 优化前：N+1 查询问题，每条记录执行一次数据库查询
     * 优化后：批量查询，将 N 次查询优化为 1 次（或少数几次）
     *
     * @param database   数据库名
     * @param columnInfo quote 字段信息
     * @param relation   关联关系
     * @param list       待处理的数据列表
     */
    private void process_quote_column(String database,
                                      AppTableColumnInfo columnInfo,
                                      AppTableRelation relation,
                                      List<Map<String, Object>> list) {
        String tableName = systemBusinessService.getAppTableNameById(relation.getPrimaryTableId());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        String primaryKey = systemBusinessService.getAppTablePrimaryKey(database, tableName);

        // 查询出 quote表的 资源类型
        List<AppTableColumnInfo> columnInfos = dynamicInfoCache.getAppTableColumnInfo(database, tableName);

        // 步骤1: 收集所有需要查询的 quote ID（去重 + 过滤 null）
        List<Object> quoteIds = list.stream()
                .map(data -> data.get(columnInfo.getColumnName()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (quoteIds.isEmpty()) {
            log.debug("No quote IDs to process for column: {}", columnInfo.getColumnName());
            return;
        }

        log.info("Batch querying {} quote records for column: {}, table: {}, ids count: {}", tableName, columnInfo.getColumnName(), database, quoteIds.size());

        // 步骤2: 批量查询关联数据（支持大数据量分批）
        List<Map<String, Object>> allQuoteData = batchQueryQuoteData(database, tableName, primaryKey, quoteIds);

        if (allQuoteData.isEmpty()) {
            log.warn("No quote data found for column: {}, table: {}", columnInfo.getColumnName(), tableName);
            return;
        }

        // 步骤3: 处理关联表的资源字段
        process_resource_column(database, tableName, columnInfos, allQuoteData);

        // 步骤4: 建立索引 Map，key 为主键值，便于快速查找
        Map<Object, Map<String, Object>> quoteDataMap = allQuoteData.stream()
                .collect(Collectors.toMap(
                        m -> m.get(primaryKey),
                        m -> m,
                        (existing, replacement) -> {
                            log.warn("Duplicate quote data found for key: {}, keeping first one", existing.get(primaryKey));
                            return existing;
                        }
                ));

        // 步骤5: 填充数据到原始 list
        for (Map<String, Object> data : list) {
            Object columnValue = data.get(columnInfo.getColumnName());
            if (Objects.isNull(columnValue)) {
                continue;
            }

            Map<String, Object> quoteData = quoteDataMap.get(columnValue);
            if (quoteData != null) {
                // 将引用字段的完整数据放入 {columnName}_map
                data.put(columnInfo.getColumnName() + "_map", quoteData);
            } else {
                log.debug("Quote data not found for column: {}, value: {}", columnInfo.getColumnName(), columnValue);
            }
        }
    }

    /**
     * 批量查询 quote 数据，支持大数据量分批查询
     * 单次 IN 查询建议不超过 1000 条，超过则分批处理
     *
     * @param database   数据库名
     * @param tableName  表名
     * @param primaryKey 主键字段名
     * @param quoteIds   需要查询的 ID 列表
     * @return 查询结果列表
     */
    private List<Map<String, Object>> batchQueryQuoteData(String database,
                                                          String tableName,
                                                          String primaryKey,
                                                          List<Object> quoteIds) {
        final int BATCH_SIZE = 1000;
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < quoteIds.size(); i += BATCH_SIZE) {
            List<Object> batchIds = quoteIds.subList(i, Math.min(i + BATCH_SIZE, quoteIds.size()));
            Map<String, Object> params = Collections.singletonMap(primaryKey, batchIds);
            List<Map<String, Object>> batchResult = dynamicMapper.list(database, tableName, params);
            if (CollectionUtils.isNotEmpty(batchResult)) {
                result.addAll(batchResult);
            }
        }

        return result;
    }

    private void mask_sensitive_fields(List<Map<String, Object>> list, String appId) {

        AppInfo appInfo = Optional.ofNullable(DynamicAuthFilter.getAppInfo())
                .orElseGet(() -> systemBusinessService.getAppInfo(appId));

        if (appInfo == null || StringUtils.isBlank(appInfo.getConfigJson())) {
            return;
        }

        Map<String, Object> configMap = JSON.parseObject(appInfo.getConfigJson(), config_value_type);

        List<String> sensitiveKeys = Optional.ofNullable((List<String>) configMap.get("encryption_config"))
                .orElse(Collections.emptyList());

        if (sensitiveKeys.isEmpty()) {
            return;
        }

        for (Map<String, Object> row : list) {
            Object name = row.get("name");
            if (name != null && sensitiveKeys.contains(name.toString())) {
                row.put("content", "******");
            }
        }
    }


    private void normalizeResource(Map<String, Object> resource) {
        String resourcePath = Convert.toStr(resource.get("resource_path"));
        resource.put("url", resourcePath);

        String resourceName = resourcePath;
        if (StringUtils.isNotEmpty(resourcePath) && resourcePath.contains("/")) {
            resourceName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
            if (resourceName.contains("?")) {
                resourceName = resourceName.substring(0, resourceName.indexOf("?"));
            }
        }
        resource.put("name", resourceName);
    }


    public long addBatch(String database, String table, List<Map<String, Object>> conditions) {
        //所有字段
        List<AppTableColumnInfo> columnInfos = dynamicInfoCache.getAppTableColumnInfo(database, table);
        final AppTableColumnInfo primaryColumn = columnInfos.stream().filter(AppTableColumnInfo::isPrimary).findFirst().get();
        List<AppTableColumnInfo> resourceColumns = columnInfos.stream()
                .filter(p -> StringUtils.equalsAnyIgnoreCase(p.getDslType(), "image", "images", "file", "files", "video", "videos"))
                .collect(Collectors.toList());

        final int insertBatch = dynamicMapper.insertBatch(database, table, conditions);


        return insertBatch;
    }


    public long deleteBatch(String database, String table, Map<String, Object> conditions) {

        final int deleteBatch = dynamicMapper.deleteBatch(database, table, conditions);

        return deleteBatch;
    }
}
