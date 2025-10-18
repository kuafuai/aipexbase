package com.kuafuai.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.event.EventService;
import com.kuafuai.common.event.EventVo;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.RandomStringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.dynamic.service.DynamicService;
import com.kuafuai.manage.entity.vo.APIKeyVo;
import com.kuafuai.manage.entity.vo.AppVo;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.util.Table2SQL;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.*;
import com.kuafuai.system.mapper.DatabaseMapper;
import com.kuafuai.system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.kuafuai.manage.service.ManageConstants.*;
import static com.kuafuai.manage.util.Table2SQL.resolveColumnType;
import static com.kuafuai.manage.util.Table2SQL.resolveDslType;

@Service
public class ManageBusinessService {

    private final static String CURRENT = "current";
    private final static String PAGE_SIZE = "pageSize";

    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("number", "INT");
        TYPE_MAP.put("quote", "INT");
        Arrays.asList("password", "email", "phone", "orders",
                        "images", "videos", "files",
                        "image", "video", "file",
                        "ai", "formula", "string", "str", "text", "keyword")
                .forEach(t -> TYPE_MAP.put(t, "VARCHAR(512)"));
        TYPE_MAP.put("fulltext", "TEXT");
    }

    @Autowired
    private UsersService usersService;
    @Autowired
    private AppInfoService appInfoService;
    @Autowired
    private EventService eventService;

    @Autowired
    private DatabaseMapper databaseMapper;

    @Autowired
    private AppTableInfoService appTableInfoService;
    @Autowired
    private AppTableColumnInfoService appTableColumnInfoService;
    @Autowired
    private AppTableRelationService appTableRelationService;
    @Autowired
    private DynamicApiSettingService dynamicApiSettingService;
    @Autowired
    private AppRequirementSQLService appRequirementSQLService;

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;
    @Autowired
    private DynamicService dynamicService;

    @Autowired
    private DynamicInfoCache dynamicInfoCache;
    @Autowired
    private SystemBusinessService systemBusinessService;

    @Autowired
    private ManageSQLBusinessService manageSQLBusinessService;


    @Resource
    private ApplicationAPIKeysService applicationAPIKeysService;

    public Users getByEmail(String email) {
        LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Users::getEmail, email);

        return usersService.getOne(queryWrapper);
    }

    public boolean register(Users users) {
        String email = users.getEmail();
        Users current = getByEmail(email);
        if (current != null) {
            return false;
        }
        users.setPassword(SecurityUtils.encryptPassword(users.getPassword()));
        boolean flag = usersService.save(users);
        if (flag) {
            eventService.publishEvent(EventVo.builder().tableName(email).model(ManageConstants.EVENT_REGISTER).build());
        }
        return flag;
    }

    /**
     * 创建-应用
     */
    public AppInfo createApp(AppVo appVo) {
        Long owner = SecurityUtils.getUserId();
        return createAppInternal(appVo.getName(), owner);
    }

    public AppInfo createApp(String name, Long owner) {
        return createAppInternal(name, owner);
    }

    /**
     * 删除应用
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(String appId) {

        databaseMapper.deleteDatabase(appId);

        appInfoService.deleteByAppId(appId);
        appRequirementSQLService.deleteByAppId(appId);
        appTableInfoService.deleteByAppId(appId);
        appTableColumnInfoService.deleteByAppId(appId);
        appTableRelationService.deleteByAppId(appId);
        dynamicApiSettingService.deleteByAppId(appId);

        dynamicInfoCache.clean(appId);
    }

    /**
     * 获取-应用下的所有表
     */
    public List<AppTableInfo> getTablesByAppId(TableVo tableVo) {
        List<String> systemTables = Arrays.asList(TABLE_STATIC_RESOURCES, TABLE_LOGIN, TABLE_SYSTEM_CONFIG, TABLE_DELAYED_TASKS);

        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, tableVo.getAppId());
        if (StringUtils.isNotEmpty(tableVo.getTableName())) {
            queryWrapper.like(AppTableInfo::getTableName, tableVo.getTableName());
        }
        List<AppTableInfo> tableInfos = appTableInfoService.list(queryWrapper);
        return tableInfos.stream().filter(table -> !systemTables.contains(table.getTableName())).collect(Collectors.toList());
    }

    /**
     * 获取表-所有字段
     */
    public List<ColumnVo> getColumnsByAppIdAndTableId(ColumnVo columnVo) {
        LambdaQueryWrapper<AppTableColumnInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableColumnInfo::getAppId, columnVo.getAppId());
        queryWrapper.eq(AppTableColumnInfo::getTableId, columnVo.getTableId());

        // 3. 查询外键关系
        List<AppTableRelation> relations = dynamicInfoCache.getTableRelations(columnVo.getAppId(), columnVo.getTableName());
        Map<Long, String> relationTableMap = buildRelationTableMap(relations);


        List<AppTableColumnInfo> columnInfos = appTableColumnInfoService.list(queryWrapper);
        return columnInfos.stream()
                .map(col -> toColumnVo(col, relationTableMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取应用-表的数据
     */
    public Page<?> getTableData(TableVo tableVo) {
        Map<String, Object> conditions = Maps.newHashMap();
        conditions.put(CURRENT, tableVo.getCurrent());
        conditions.put(PAGE_SIZE, tableVo.getPageSize());

        return dynamicInterfaceService.page(tableVo.getAppId(), tableVo.getTableName(), conditions);
    }

    /**
     * 获取表的select数据
     */
    public List<Map<String, Object>> getTableSelect(TableVo tableVo) {

        List<AppTableColumnInfo> columns = dynamicInfoCache.getAppTableColumnInfo(tableVo.getAppId(), tableVo.getTableName());
        AppTableColumnInfo targetColumn = columns.stream()
                .filter(AppTableColumnInfo::isShow)
                .findFirst()
                .orElseGet(() -> columns
                        .stream()
                        .filter(AppTableColumnInfo::isPrimary)
                        .findFirst()
                        .orElseGet(() -> columns.get(0))
                );

        Map<String, Object> conditions = Maps.newHashMap();
        if (targetColumn != null) {
            conditions.put("select_show_name", targetColumn.getColumnName());
        }

        return dynamicInterfaceService.list(tableVo.getAppId(), tableVo.getTableName(), conditions);
    }


    public TableVo getTable(TableVo queryVo) {

        // 1. 查询表信息
        AppTableInfo tableInfo = appTableInfoService.getOne(
                new LambdaQueryWrapper<AppTableInfo>()
                        .eq(AppTableInfo::getAppId, queryVo.getAppId())
                        .eq(AppTableInfo::getTableName, queryVo.getTableName())
        );

        if (tableInfo == null) {
            throw new BusinessException("表不存在：" + queryVo.getTableName());
        }

        return tableInfo2Vo(tableInfo);
    }

    public List<TableVo> getTablesByAppId(String appId) {
        if (StringUtils.isEmpty(appId)) {
            return null;
        }
        // 1. 查询表信息
        List<AppTableInfo> tables = getTablesByAppId(TableVo.builder().appId(appId).build());
        return tables.stream().map(this::tableInfo2Vo).collect(Collectors.toList());
    }

    private TableVo tableInfo2Vo(AppTableInfo tableInfo) {
        String appId = tableInfo.getAppId();
        Long tableId = tableInfo.getId();

        // 2. 查询字段信息
        List<AppTableColumnInfo> columnInfos = appTableColumnInfoService.list(
                new LambdaQueryWrapper<AppTableColumnInfo>()
                        .eq(AppTableColumnInfo::getAppId, appId)
                        .eq(AppTableColumnInfo::getTableId, tableId)
        );

        // 3. 查询外键关系
        List<AppTableRelation> relations = dynamicInfoCache.getTableRelations(appId, tableInfo.getTableName());
        Map<Long, String> relationTableMap = buildRelationTableMap(relations);

        List<ColumnVo> columnVos = columnInfos.stream()
                .map(col -> toColumnVo(col, relationTableMap))
                .collect(Collectors.toList());

        return TableVo.builder()
                .id(tableInfo.getId())
                .appId(appId)
                .description(tableInfo.getDescription())
                .tableName(tableInfo.getTableName())
                .columns(columnVos)
                .build();
    }

    /**
     * 删除表
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTable(String appId, TableVo tableVo) {

        String tableName = systemBusinessService.getAppTableNameByIdAndAppId(tableVo.getId(), appId);
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        String ddlSQL = generateCreateTableSQL(appId, tableName);
        // 4. 执行DDL
        boolean ddlExecuted = manageSQLBusinessService.execute(appId, ddlSQL);
        if (!ddlExecuted) {
            return false;
        }

        appTableInfoService.deleteByAppIdAndTableName(appId, tableName);
        appTableColumnInfoService.deleteByAppIdAndTableId(appId, tableVo.getId());
        appTableRelationService.deleteByAppIdAndTableId(appId, tableVo.getId());
        dynamicInfoCache.clean(appId);

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateTable(String appId, TableVo tableVo) {
        // 1. 查询旧表信息
        AppTableInfo oldTableInfo = appTableInfoService.lambdaQuery()
                .eq(AppTableInfo::getAppId, appId)
                .eq(AppTableInfo::getTableName, tableVo.getTableName())
                .one();

        if (oldTableInfo == null) {
            throw new BusinessException("表不存在：" + tableVo.getTableName());
        }

        // 2. 查询旧列
        List<AppTableColumnInfo> oldColumns = appTableColumnInfoService.lambdaQuery()
                .eq(AppTableColumnInfo::getAppId, appId)
                .eq(AppTableColumnInfo::getTableId, oldTableInfo.getId())
                .list();

        Map<String, AppTableColumnInfo> oldColumnMap = oldColumns.stream()
                .collect(Collectors.toMap(AppTableColumnInfo::getColumnName, Function.identity()));

        // 3. 处理类型
        processColumnType(tableVo);

        // 4. 生成 ALTER TABLE SQL 列表
        List<String> alterSQLs = generateAlterTableSQL(appId, tableVo, oldColumnMap);

        // 没有变化就不执行
        if (alterSQLs.isEmpty()) {
            dynamicInfoCache.clean(appId);
            return true;
        }

        // 5. 执行 DDL
        boolean ddlExecuted = manageSQLBusinessService.execute(appId, alterSQLs);
        if (!ddlExecuted) {
            return false;
        }

        // 6. 更新元数据
        // 删除旧字段，重新保存最新的列结构
        appTableColumnInfoService.deleteByAppIdAndTableId(appId, oldTableInfo.getId());
        appTableRelationService.deleteByAppIdAndTableId(appId, oldTableInfo.getId());

        saveColumnMetadata(appId, oldTableInfo.getId(), tableVo);

        // 7. 更新表描述（如果有变化）
        if (StringUtils.isNotEmpty(tableVo.getDescription())) {
            oldTableInfo.setDescription(tableVo.getDescription());
            appTableInfoService.updateById(oldTableInfo);
        }

        // 8. 重新处理表关系（引用字段）
        processTableRelations(appId, Collections.singletonList(tableVo));

        // 9.清空缓存
        dynamicInfoCache.clean(appId);
        return true;
    }

    /**
     * 创建一张表
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createTable(String appId, TableVo tableVo) {
        return createTables(appId, Collections.singletonList(tableVo));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean createTables(String appId, List<TableVo> tableVos) {
        // 1. 校验表名是否重复
        validateDuplicateTables(appId, tableVos);

        // 2. 处理列类型
        tableVos.forEach(this::processColumnType);

        // 3. 生成 DDL
        List<String> ddlSQLs = generateCreateTableSQLs(appId, tableVos);

        // 4. 执行DDL
        boolean ddlExecuted = manageSQLBusinessService.execute(appId, ddlSQLs);
        if (!ddlExecuted) {
            return false;
        }

        // 5. 落库：表信息 + 字段信息
        for (TableVo tableVo : tableVos) {
            AppTableInfo tableInfo = saveTableMetadata(appId, tableVo);
            saveColumnMetadata(appId, tableInfo.getId(), tableVo);
        }

        // 6. 处理表关系
        processTableRelations(appId, tableVos);
        return true;
    }


    /**
     * 添加数据
     */
    public BaseResponse saveData(String appId, String tableName, Map<String, Object> data) {
        return dynamicService.add(appId, tableName, data);
    }

    public BaseResponse updateData(String appId, String tableName, Map<String, Object> data) {
        return dynamicService.update(appId, tableName, data);
    }

    /**
     * 删除数据
     */
    public BaseResponse deleteData(String appId, String tableName, Map<String, Object> data) {
        String primaryKey = systemBusinessService.getAppTablePrimaryKey(appId, tableName);
        if (StringUtils.isNotEmpty(primaryKey)) {
            Map<String, Object> primaryMap = Maps.newHashMap();
            primaryMap.put(primaryKey, data.get(primaryKey));

            return dynamicService.delete(appId, tableName, primaryMap);
        } else {
            return dynamicService.delete(appId, tableName, data);
        }
    }


    private void validateDuplicateTables(String appId, List<TableVo> tableVos) {
        for (TableVo vo : tableVos) {
            if (appTableInfoService.existTableNameByAppId(appId, vo.getTableName())) {
                throw new BusinessException("表 " + vo.getTableName() + " 已存在，请勿重复创建");
            }
        }
    }

    private List<String> generateCreateTableSQLs(String appId, List<TableVo> tables) {
        return tables.stream()
                .map(table -> Table2SQL.table2SQL(appId, table))
                .collect(Collectors.toList());
    }

    private String generateCreateTableSQL(String appId, String tableName) {
        return "DROP TABLE IF EXISTS `" + appId + "`." + tableName + ";";
    }

    private List<String> generateAlterTableSQL(String appId, TableVo newTable, Map<String, AppTableColumnInfo> oldColumns) {
        List<String> sqls = new ArrayList<>();
        String fullTableName = "`" + appId + "`.`" + newTable.getTableName() + "`";

        for (ColumnVo newCol : newTable.getColumns()) {
            String colName = newCol.getColumnName();
            AppTableColumnInfo oldCol = oldColumns.get(colName);

            // 1. 新增列
            if (oldCol == null) {
                String addSql = String.format("ALTER TABLE %s ADD COLUMN %s;", fullTableName, generateColumnDefinition(newCol));
                sqls.add(addSql);
                continue;
            }

            // 2. 修改列（字段类型、注释、是否可空变化）
            boolean changed = false;
            if (!StringUtils.equalsIgnoreCase(oldCol.getColumnType(), newCol.getColumnType())) changed = true;
            if (!StringUtils.equalsIgnoreCase(oldCol.getColumnComment(), newCol.getColumnComment())) changed = true;
            if (!oldCol.isPrimary()) {
                if (oldCol.isNullable() != !newCol.getIsNullable()) changed = true;
            }

            if (changed) {
                String modifySql = String.format("ALTER TABLE %s MODIFY COLUMN %s;", fullTableName,
                        generateColumnDefinition(newCol));
                sqls.add(modifySql);
            }

        }

        // 3. 删除旧列
        for (String oldColName : oldColumns.keySet()) {
            boolean stillExists = newTable.getColumns().stream()
                    .anyMatch(c -> c.getColumnName().equalsIgnoreCase(oldColName));
            if (!stillExists) {
                String dropSql = String.format("ALTER TABLE %s DROP COLUMN `%s`;", fullTableName, oldColName);
                sqls.add(dropSql);
            }
        }

        return sqls;
    }

    private String generateColumnDefinition(ColumnVo column) {
        // 直接复用 Table2SQL 的实现
        return Table2SQL.resolveColumnType(column) != null
                ? Table2SQL.generateColumnDefinition(column)
                : "`" + column.getColumnName() + "` " + column.getColumnType();
    }

    /**
     * 保存表元数据
     */
    private AppTableInfo saveTableMetadata(String appId, TableVo tableVo) {
        AppTableInfo appTableInfo = AppTableInfo.builder()
                .appId(appId)
                .tableName(tableVo.getTableName())
                .physicalTableName(tableVo.getTableName())
                .description(tableVo.getDescription())
                .requirementId(ManageConstants.REQUIREMENT_DEFAULT_ID)
                .build();
        appTableInfoService.save(appTableInfo);
        return appTableInfo;
    }

    /**
     * 保存列元数据
     */
    private void saveColumnMetadata(String appId, Long tableId, TableVo tableVo) {
        List<AppTableColumnInfo> columnInfos = tableVo.getColumns().stream()
                .map(c -> transformColumn(appId, tableId, c))
                .collect(Collectors.toList());
        appTableColumnInfoService.saveBatch(columnInfos);
    }

    private AppTableColumnInfo transformColumn(String appId, Long tableId, ColumnVo columnVo) {
        String columnName = columnVo.getColumnName();
        String columnType = resolveColumnType(columnVo);
        String dslType = resolveDslType(columnVo);

        boolean isPrimary = columnVo.getIsPrimary();
        boolean isNullable = !columnVo.getIsNullable();

        if (isPrimary) {
            isNullable = false;
        }

        return AppTableColumnInfo.builder()
                .appId(appId)
                .requirementId(ManageConstants.REQUIREMENT_DEFAULT_ID)
                .tableId(tableId)
                .columnName(columnName)
                .columnType(columnType)
                .dslType(dslType)
                .isPrimary(isPrimary)
                .isNullable(isNullable)
                .isShow(Objects.nonNull(columnVo.getIsShow()) ? columnVo.getIsShow() : false)
                .columnComment(columnVo.getColumnComment())
                .build();
    }

    private void processTableRelations(String appId, List<TableVo> tableVos) {

        // 查询所有表
        Map<String, AppTableInfo> tableMap = getAllTablesByAppId(appId).stream()
                .collect(Collectors.toMap(AppTableInfo::getTableName, Function.identity()));

        // 查询所有列，并按表ID分组
        Map<Long, List<AppTableColumnInfo>> columnMap = getAllColumnsByAppId(appId).stream()
                .collect(Collectors.groupingBy(AppTableColumnInfo::getTableId));


        for (TableVo tableVo : tableVos) {

            List<ColumnVo> quoteColumns = tableVo.getColumns().stream()
                    .filter(c -> StringUtils.equalsIgnoreCase(c.getDslType(), "quote"))
                    .collect(Collectors.toList());

            if (quoteColumns.isEmpty()) continue;

            //1. 查询主表
            AppTableInfo tableInfo = tableMap.get(tableVo.getTableName());
            if (tableInfo == null) continue;

            //2. 主表所有列
            Map<String, AppTableColumnInfo> currentColumns = columnMap.getOrDefault(tableInfo.getId(), Lists.newArrayList())
                    .stream().collect(Collectors.toMap(AppTableColumnInfo::getColumnName, Function.identity()));

            for (ColumnVo columnVo : quoteColumns) {
                AppTableColumnInfo column = currentColumns.get(columnVo.getColumnName());
                if (column == null)
                    continue;
                //3. 查询引用表
                AppTableInfo refTable = tableMap.get(columnVo.getReferenceTableName());
                if (refTable == null)
                    continue;

                //4. 查询引用表的所有列，找出主键
                AppTableColumnInfo refPrimary = columnMap.getOrDefault(refTable.getId(), Lists.newArrayList())
                        .stream().filter(AppTableColumnInfo::isPrimary).findFirst().orElse(null);
                if (refPrimary == null) continue;

                AppTableRelation relation = AppTableRelation.builder()
                        .appId(appId)
                        .requirementId(ManageConstants.REQUIREMENT_DEFAULT_ID)
                        .tableId(tableInfo.getId())
                        .tableColumnId(column.getId())
                        .primaryTableId(refTable.getId())
                        .primaryTableColumnId(refPrimary.getId())
                        .relationType(ManageConstants.RELATION_TYPE_ONE_TO_ONE)
                        .build();
                appTableRelationService.save(relation);
            }
        }
    }

    /**
     * 获取 appId 下所有表
     */
    private List<AppTableInfo> getAllTablesByAppId(String appId) {
        return appTableInfoService.list(
                new LambdaQueryWrapper<AppTableInfo>().eq(AppTableInfo::getAppId, appId)
        );
    }

    /**
     * 获取 appId 下所有列
     */
    private List<AppTableColumnInfo> getAllColumnsByAppId(String appId) {
        return appTableColumnInfoService.list(
                new LambdaQueryWrapper<AppTableColumnInfo>().eq(AppTableColumnInfo::getAppId, appId)
        );
    }


    private void processColumnType(TableVo tableVo) {
        tableVo.getColumns().forEach(p -> {
            String originalType = p.getColumnType();
            String normalizedType = originalType.toLowerCase();
            p.setDslType(originalType);
            p.setColumnType(TYPE_MAP.getOrDefault(normalizedType, originalType));
        });
    }


    private Map<Long, String> buildRelationTableMap(List<AppTableRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }
        return relations.stream()
                .collect(Collectors.toMap(
                        AppTableRelation::getTableColumnId,
                        rel -> systemBusinessService.getAppTableNameById(rel.getPrimaryTableId()),
                        (v1, v2) -> v1 // 若重复，保留第一个
                ));
    }

    private ColumnVo toColumnVo(AppTableColumnInfo column, Map<Long, String> relationTableMap) {
        String showName = column.getColumnName();
        if (relationTableMap.containsKey(column.getId())) {
            //说明有外键关系
            List<AppTableColumnInfo> columns = dynamicInfoCache.getAppTableColumnInfo(column.getAppId(), relationTableMap.get(column.getId()));
            if (!columns.isEmpty()) {
                AppTableColumnInfo targetColumn = columns.stream()
                        .filter(AppTableColumnInfo::isShow)
                        .findFirst()
                        .orElseGet(() -> columns
                                .stream()
                                .filter(AppTableColumnInfo::isPrimary)
                                .findFirst()
                                .orElseGet(() -> columns.get(0))
                        );
                showName = targetColumn.getColumnName();
            }
        }

        return ColumnVo.builder()
                .id(column.getId())
                .appId(column.getAppId())
                .tableId(column.getTableId())
                .columnName(column.getColumnName())
                .columnComment(column.getColumnComment())
                .columnType(column.getDslType())
                .dslType(column.getDslType())
                .isPrimary(column.isPrimary())
                .isNullable(!column.isNullable())
                .isShow(column.isShow())
                .referenceTableName(relationTableMap.getOrDefault(column.getId(), ""))
                .showName(showName)
                .build();
    }

    /**
     * 创建 api key
     */

    public boolean saveAPIKey(String appId, APIKeyVo apiKeyVo) {
        String keyName = "kf_api_" + RandomStringUtils.generateRandomString(32);

        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(APIKey::getAppId, appId);
        queryWrapper.eq(APIKey::getStatus, APIKey.APIKeyStatus.ACTIVE.name());
        queryWrapper.ge(APIKey::getExpireAt, DateUtils.getTime());

        long count = applicationAPIKeysService.count(queryWrapper);

        if (count > 10) {
            return false;
        }

        APIKey apiKeys = APIKey.builder()
                .appId(appId)
                .name(apiKeyVo.getName())
                .keyName(keyName)
                .description(apiKeyVo.getDescription())
                .status(APIKey.APIKeyStatus.ACTIVE.name())
                .expireAt(StringUtils.isEmpty(apiKeyVo.getExpireAt()) ? "2099-12-31 23:59:59" : apiKeyVo.getExpireAt())
                .createAt(DateUtils.getTime())
                .build();

        return applicationAPIKeysService.save(apiKeys);
    }

    public boolean executeDDL(String appId, String ddl) {
        return true;
    }


    private AppInfo createAppInternal(String name, Long owner) {
        String appId = "baas_" + RandomStringUtils.generateRandomString(16);
        AppInfo appInfo = new AppInfo();
        appInfo.setAppId(appId);
        appInfo.setAppName(name);
        appInfo.setNeedAuth(false);
        appInfo.setConfigJson("{}");
        appInfo.setStatus(ManageConstants.STATUS_DRAFT);
        appInfo.setOwner(owner);
        appInfoService.save(appInfo);

        eventService.publishEvent(EventVo.builder().appId(appInfo.getAppId()).model(ManageConstants.EVENT_CREATE).build());
        return appInfo;
    }

}
