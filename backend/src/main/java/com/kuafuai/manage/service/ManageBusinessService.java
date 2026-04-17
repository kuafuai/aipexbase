package com.kuafuai.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.kuafuai.manage.entity.dto.AppInfoCopyDTO;
import com.kuafuai.manage.entity.vo.APIKeyVo;
import com.kuafuai.manage.entity.vo.AppBatchVo;
import com.kuafuai.manage.entity.vo.AppVo;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.util.Table2SQL;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.SystemBusinessService;
import com.kuafuai.system.entity.*;
import com.kuafuai.system.mapper.DatabaseMapper;
import com.kuafuai.system.service.*;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    @Autowired
    private RlsPolicyService rlsPolicyService;

    @Autowired
    private ApiMarketService apiMarketService;

    @Transactional
    public void recycle(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            return;
        }
//
//        appInfo.setStatus("recycle");
//        appInfoService.updateById(appInfo);

        // drop database
        databaseMapper.deleteDatabase(appId);

        // delete record
        appInfoService.deleteByAppId(appId);
        appRequirementSQLService.deleteByAppId(appId);
        appTableInfoService.deleteByAppId(appId);
        appTableColumnInfoService.deleteByAppId(appId);
        appTableRelationService.deleteByAppId(appId);
        dynamicApiSettingService.deleteByAppId(appId);

        applicationAPIKeysService.deleteByAppId(appId);

        dynamicInfoCache.clean(appId);
    }

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

    /**
     * 批量创建应用和表 - 用于对外API
     *
     * @param appBatchVo 包含应用信息和表列表的请求对象
     * @return 创建的应用信息
     */
    public AppInfo createAppWithTables(AppBatchVo appBatchVo) {
        // 1. 创建应用
        List<String> readTables = appBatchVo.getTables() == null ? null
                : appBatchVo.getTables().stream().map(TableVo::getTableName).collect(Collectors.toList());
        AppInfo appInfo = createAppForApi(appBatchVo.getName(), appBatchVo.getUserId(), appBatchVo.getNeedAuth(), appBatchVo.getAuthTable(), readTables);

        // 3. 如果有表信息，批量创建表
        if (appBatchVo.getTables() != null && !appBatchVo.getTables().isEmpty()) {

            // 显式同步创建数据库，避免异步创建导致的时序问题
            databaseMapper.createDatabase(appInfo.getAppId());

            createTables(appInfo.getAppId(), appBatchVo.getTables());
        }

        // 3. 如果有 RLS 策略，批量创建策略
        if (appBatchVo.getRlsPolicies() != null && !appBatchVo.getRlsPolicies().isEmpty()) {
            for (String policySql : appBatchVo.getRlsPolicies()) {
                try {
                    rlsPolicyService.createFromSql(appInfo.getAppId(), policySql, null);
                    log.info("RLS 策略创建成功: appId={}, sql={}", appInfo.getAppId(), policySql);
                } catch (Exception e) {
                    log.error("RLS 策略创建失败: appId={}, sql={}", appInfo.getAppId(), policySql, e);
                    throw new BusinessException("创建 RLS 策略失败: " + e.getMessage(), e);
                }
            }
        }

        return appInfo;
    }

    /**
     * 创建应用 - 用于对外API
     *
     * @param name           应用名称
     * @param externalUserId 外部平台的用户ID
     * @return 创建的应用信息
     */
    public AppInfo createAppForApi(String name, String externalUserId) {
        // 根据外部用户ID获取或创建本地用户
        Long owner = getOrCreateUserByExternalId(externalUserId);
        return createAppInternal(name, owner);
    }

    public AppInfo createAppForApi(String name, String externalUserId, Boolean needAuth, String authTable, List<String> readTables) {
        // 根据外部用户ID获取或创建本地用户
        Long owner = getOrCreateUserByExternalId(externalUserId);
        return createAppInternal(name, owner, needAuth, authTable, readTables);
    }

    /**
     * 根据外部用户ID获取或创建本地用户
     *
     * @param externalUserId 外部平台的用户ID
     * @return 本地用户ID
     */
    private Long getOrCreateUserByExternalId(String externalUserId) {
        // 查询用户是否存在
        LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Users::getCodeFlyingUserId, externalUserId);
        Users existingUser = usersService.getOne(wrapper);

        if (existingUser != null) {
            log.info("用户已存在 externalUserId={}, userId={}", externalUserId, existingUser.getId());
            return existingUser.getId();
        }

        // 用户不存在，创建新用户
        Users newUser = Users.builder()
                .email("external_" + externalUserId + "@external.com")
                .codeFlyingUserId(externalUserId)
                .nickName("外部用户_" + externalUserId)
                .build();
        usersService.save(newUser);
        log.info("创建新用户 externalUserId={}, userId={}", externalUserId, newUser.getId());

        return newUser.getId();
    }

    /**
     * 复制应用
     */
    @Transactional(rollbackFor = Exception.class)
    public AppInfo copyApp(String appId) {

        // ===== 1. 校验原应用 =====
        AppInfo originalApp = appInfoService.getAppInfoByAppId(appId);
        if (originalApp == null) {
            throw new BusinessException("原应用不存在：" + appId);
        }

        // ===== 2. 解析原应用表数据结构 =====
        AppInfoCopyDTO appStructure = parseAppStructure(appId);


        // ===== 4. 执行创建库表操作 =====
        return createAppTablesFromStructure(null, appStructure);
    }

    /**
     * 解析应用结构
     */
    public AppInfoCopyDTO parseAppStructure(String appId) {
        // 校验应用
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("应用不存在：" + appId);
        }
        log.info("应用结构解析结果：{}", appInfo);

        // 获取表数据结构
        List<AppTableInfo> tables = appTableInfoService.list(
                new LambdaQueryWrapper<AppTableInfo>()
                        .eq(AppTableInfo::getAppId, appId)
        );

        List<AppTableColumnInfo> columns = appTableColumnInfoService.list(
                new LambdaQueryWrapper<AppTableColumnInfo>()
                        .eq(AppTableColumnInfo::getAppId, appId)
        );

        Map<Long, List<AppTableColumnInfo>> tableColumnMap =
                columns.stream()
                        .collect(Collectors.groupingBy(AppTableColumnInfo::getTableId));

        List<AppTableRelation> relations = appTableRelationService.list(
                new LambdaQueryWrapper<AppTableRelation>()
                        .eq(AppTableRelation::getAppId, appId)
        );

        // 获取SQL信息
        List<AppRequirementSQL> sqls = appRequirementSQLService.list(
                new LambdaQueryWrapper<AppRequirementSQL>()
                        .ne(AppRequirementSQL::getStatus, "delete")
                        .eq(AppRequirementSQL::getAppId, appId)
        );

        return AppInfoCopyDTO.builder()
                .appInfo(appInfo)
                .tables(tables)
                .tableColumnMap(tableColumnMap)
                .relations(relations)
                .sqls(sqls)
                .build();
    }

    /**
     * 从应用结构创建应用表
     */
    @Transactional(rollbackFor = Exception.class)
    public AppInfo createAppTablesFromStructure(String targetAppId, AppInfoCopyDTO appStructure) {
        List<AppTableInfo> originalTables = appStructure.getTables();
        Map<Long, List<AppTableColumnInfo>> tableColumnMap = appStructure.getTableColumnMap();
        List<AppTableRelation> originalRelations = appStructure.getRelations();
        List<AppRequirementSQL> originalSqls = appStructure.getSqls();

        AppInfo originalAppInfo = appStructure.getAppInfo();
        String originalAppId = originalAppInfo.getAppId();
        if (StringUtils.isEmpty(targetAppId)) {
            targetAppId = "baas_" + RandomStringUtils.generateRandomString(16);
        }

        // ===== ID 映射 =====
        Map<Long, Long> tableIdMap = new HashMap<>();
        Map<Long, Long> columnIdMap = new HashMap<>();

        try {
            // 创建新数据库
            databaseMapper.createDatabase(targetAppId);

            // 保存新应用
            originalAppInfo.setAppId(targetAppId);
            originalAppInfo.setId(null);
            appInfoService.save(originalAppInfo);

            // ===== 复制表 & 字段（一次构建映射）=====
            for (AppTableInfo oldTable : originalTables) {

                AppTableInfo newTable = new AppTableInfo();
                newTable.setAppId(targetAppId);
                newTable.setTableName(oldTable.getTableName());
                newTable.setPhysicalTableName(oldTable.getPhysicalTableName());
                newTable.setDescription(oldTable.getDescription());
                newTable.setRequirementId(oldTable.getRequirementId());
                appTableInfoService.save(newTable);

                tableIdMap.put(oldTable.getId(), newTable.getId());

                List<AppTableColumnInfo> cols =
                        tableColumnMap.getOrDefault(oldTable.getId(), Collections.emptyList());

                for (AppTableColumnInfo oldCol : cols) {
                    AppTableColumnInfo newCol = new AppTableColumnInfo();
                    newCol.setAppId(targetAppId);
                    newCol.setTableId(newTable.getId());
                    newCol.setRequirementId(oldCol.getRequirementId());
                    newCol.setColumnName(oldCol.getColumnName());
                    newCol.setColumnType(oldCol.getColumnType());
                    newCol.setDslType(oldCol.getDslType());
                    newCol.setPrimary(oldCol.isPrimary());
                    newCol.setNullable(oldCol.isNullable());
                    newCol.setShow(oldCol.isShow());
                    newCol.setColumnComment(oldCol.getColumnComment());
                    appTableColumnInfoService.save(newCol);

                    columnIdMap.put(oldCol.getId(), newCol.getId());
                }
            }

            // ===== 复制表关系（完全使用映射）=====
            for (AppTableRelation oldRel : originalRelations) {

                Long newTableId = tableIdMap.get(oldRel.getTableId());
                Long newColumnId = columnIdMap.get(oldRel.getTableColumnId());
                Long newPrimaryTableId = tableIdMap.get(oldRel.getPrimaryTableId());
                Long newPrimaryColumnId = columnIdMap.get(oldRel.getPrimaryTableColumnId());

                if (newTableId == null || newColumnId == null ||
                        newPrimaryTableId == null || newPrimaryColumnId == null) {
                    continue;
                }

                AppTableRelation newRel = new AppTableRelation();
                newRel.setAppId(targetAppId);
                newRel.setRequirementId(oldRel.getRequirementId());
                newRel.setTableId(newTableId);
                newRel.setTableColumnId(newColumnId);
                newRel.setPrimaryTableId(newPrimaryTableId);
                newRel.setPrimaryTableColumnId(newPrimaryColumnId);
                newRel.setRelationType(oldRel.getRelationType());
                appTableRelationService.save(newRel);
            }

            // ===== 执行 SQL（替换 appId）=====
            // 收集所有SQL内容并替换appId后批量执行，避免重复插入
            List<String> processedSqls = new ArrayList<>();
            for (AppRequirementSQL sql : originalSqls) {
                String content = sql.getContent().replace(originalAppId, targetAppId);
                processedSqls.add(content);
            }

            // 批量执行处理后的SQL，只插入一次记录
            if (!processedSqls.isEmpty()) {
                if (!manageSQLBusinessService.execute(targetAppId, processedSqls)) {
                    throw new BusinessException("SQL 批量执行失败");
                }
            }


            dynamicInfoCache.clean(targetAppId);
        } catch (Exception e) {
            deleteApp(targetAppId);
            throw new BusinessException("复制应用失败：" + e.getMessage(), e);
        }

        return originalAppInfo;
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

        applicationAPIKeysService.deleteByAppId(appId);

        // 删除 RLS 策略
        rlsPolicyService.deleteByAppId(appId);

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
     * 向已有表中添加一个字段
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addColumn(String appId, String tableName, ColumnVo columnVo) {
        // 1. 查询表信息
        AppTableInfo tableInfo = appTableInfoService.lambdaQuery()
                .eq(AppTableInfo::getAppId, appId)
                .eq(AppTableInfo::getTableName, tableName)
                .one();
        if (tableInfo == null) {
            throw new BusinessException("表不存在：" + tableName);
        }

        // 2. 检查字段是否已存在
        boolean exists = appTableColumnInfoService.lambdaQuery()
                .eq(AppTableColumnInfo::getAppId, appId)
                .eq(AppTableColumnInfo::getTableId, tableInfo.getId())
                .eq(AppTableColumnInfo::getColumnName, columnVo.getColumnName())
                .exists();
        if (exists) {
            throw new BusinessException("字段已存在：" + columnVo.getColumnName());
        }

        // 3. 设置默认值（避免后续 Boolean 拆箱 NPE）
        if (columnVo.getIsPrimary() == null) columnVo.setIsPrimary(false);
        if (columnVo.getIsNullable() == null) columnVo.setIsNullable(false);
        if (columnVo.getIsShow() == null) columnVo.setIsShow(false);

        // 4. 处理字段类型
        String originalType = columnVo.getColumnType();
        String normalizedType = originalType != null ? originalType.toLowerCase() : "";
        columnVo.setDslType(originalType);
        columnVo.setColumnType(TYPE_MAP.getOrDefault(normalizedType, originalType));

        // 5. 生成 ALTER TABLE ADD COLUMN SQL
        String fullTableName = "`" + appId + "`.`" + tableName + "`";
        String addSql = String.format("ALTER TABLE %s ADD COLUMN %s;", fullTableName, generateColumnDefinition(columnVo));

        // 6. 执行 DDL
        boolean ddlExecuted = manageSQLBusinessService.execute(appId, addSql);
        if (!ddlExecuted) {
            return false;
        }
        AppTableColumnInfo columnInfo = transformColumn(appId, tableInfo.getId(), columnVo);
        appTableColumnInfoService.save(columnInfo);

        // 7. 清空缓存
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

    private AppInfo createAppInternal(String name, Long owner, Boolean needAuth, String authTable, List<String> readTables) {
        String appId = "baas_" + RandomStringUtils.generateRandomString(16);
        AppInfo appInfo = new AppInfo();
        appInfo.setAppId(appId);
        appInfo.setAppName(name);
        appInfo.setNeedAuth(needAuth != null ? needAuth : false);
        appInfo.setAuthTable(authTable);
        appInfo.setConfigJson(buildConfigJson(readTables));
        appInfo.setStatus(ManageConstants.STATUS_DRAFT);
        appInfo.setOwner(owner);
        appInfoService.save(appInfo);

        eventService.publishEvent(EventVo.builder().appId(appInfo.getAppId()).model(ManageConstants.EVENT_CREATE).build());
        return appInfo;
    }

    private String buildConfigJson(List<String> readTables) {
        if (readTables == null || readTables.isEmpty()) {
            return "{}";
        }
        Map<String, Object> config = new HashMap<>();
        config.put("read_table", readTables);
        try {
            return new ObjectMapper().writeValueAsString(config);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("序列化 configJson 失败", e);
            return "{}";
        }
    }

    /**
     * 初始化应用的 API 配置
     * 从 ApiMarket 获取公共 API 并保存到 DynamicApiSetting
     *
     * @param appId 应用ID
     */
    public void initializeApiSettings(String appId) {
        try {
            // 获取所有ApiMarket数据
            LambdaQueryWrapper<ApiMarket> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiMarket::getStatus, 1);
            wrapper.orderByDesc(ApiMarket::getCreatedAt);
            List<ApiMarket> apiMarkets = apiMarketService.list(wrapper);

            if (apiMarkets.isEmpty()) {
                log.info("ApiMarket中没有可用的API数据 appId={}", appId);
                return;
            }

            // 转换为DynamicApiSetting并保存
            List<DynamicApiSetting> apiSettings = apiMarkets.stream()
                    .map(marketVo -> DynamicApiSetting.builder()
                            .appId(appId)
                            .marketId(marketVo.getId())
                            .keyName(marketVo.getName())
                            .description(marketVo.getDescription())
                            .url(marketVo.getUrl())
                            .token(marketVo.getToken())
                            .protocol(PROTOCOL_MAP.get(marketVo.getProtocol()))
                            .method(METHOD_MAP.get(marketVo.getMethod()))
                            .bodyTemplate(marketVo.getBodyTemplate())
                            .header(marketVo.getHeaders())
                            .dataType(DATA_TYPE_MAP.get(marketVo.getDataType()))
                            .dataPath(marketVo.getDataPath())
                            .build())
                    .collect(Collectors.toList());

            dynamicApiSettingService.saveBatch(apiSettings);
            log.info("从ApiMarket初始化API配置成功 appId={}, count={}", appId, apiSettings.size());
        } catch (Exception e) {
            log.error("从ApiMarket获取API配置失败 appId={}", appId, e);
        }
    }

    private static final Map<Integer, String> METHOD_MAP = new HashMap<Integer, String>() {{
        put(0, "GET");
        put(1, "POST");
        put(2, "PUT");
        put(3, "DELETE");
    }};

    private static final Map<Integer, String> DATA_TYPE_MAP = new HashMap<Integer, String>() {{
        put(0, "TEXT");
        put(1, "JSON");
        put(2, "ARRAY");
    }};

    private static final Map<Integer, String> PROTOCOL_MAP = new HashMap<Integer, String>() {{
        put(1, "HTTP");
        put(2, "WSS");
    }};

}
