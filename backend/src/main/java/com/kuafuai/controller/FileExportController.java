package com.kuafuai.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.base.CaseFormat;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.excel.ExcelProvider;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.dynamic.service.DynamicService;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableInfo;
import com.kuafuai.system.service.AppTableColumnInfoService;
import com.kuafuai.system.service.AppTableInfoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@AllArgsConstructor
@Slf4j
public class FileExportController {


    private static final String LOGIN_TABLE_NAME = "login";
    private static final String LOGIN_MANAGER_TABLE_NAME = "login_manger";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ExcelProvider excelProvider;
    private final AppTableInfoService appTableInfoService;
    private final AppTableColumnInfoService appTableColumnInfoService;
    private final DynamicService dynamicService;

    private String normalizeTableName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new BusinessException("error.param.required", "tableName");
        }

        // 特殊表名处理
        if (LOGIN_MANAGER_TABLE_NAME.equals(tableName)) {
            return LOGIN_TABLE_NAME;
        }

        // 驼峰转下划线
        if (!tableName.contains("_")) {
            return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, tableName);
        }

        return tableName;
    }

    /**
     * 获取表列信息
     */
    private List<AppTableColumnInfo> getTableColumnInfos(String tableName) {
        String appId = GlobalAppIdFilter.getAppId();
        AppTableInfo tableInfo = getTableInfoByTableName(tableName, appId);
        return getColumnInfosByTableId(tableInfo.getId(), appId);
    }

    /**
     * 根据表名获取表信息
     */
    private AppTableInfo getTableInfoByTableName(String tableName, String appId) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getTableName, tableName)
                .eq(AppTableInfo::getAppId, appId);

        AppTableInfo tableInfo = appTableInfoService.getOne(queryWrapper);
        if (ObjectUtils.isEmpty(tableInfo)) {
            throw new BusinessException("error.code.not_found");
        }
        return tableInfo;
    }

    /**
     * 根据表ID获取列信息
     */
    private List<AppTableColumnInfo> getColumnInfosByTableId(Long tableId, String appId) {
        LambdaQueryWrapper<AppTableColumnInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableColumnInfo::getTableId, tableId)
                .eq(AppTableColumnInfo::getAppId, appId);
        return appTableColumnInfoService.list(queryWrapper);
    }

    /**
     * 导出模板
     */
    @GetMapping("/{tableName}/downloadTemplate")
    public void downloadTemplate(@PathVariable String tableName, HttpServletResponse response) {
        try {
            String normalizedTableName = normalizeTableName(tableName);
            List<AppTableColumnInfo> columnInfos = getTableColumnInfos(normalizedTableName);

            Workbook workbook = excelProvider.downloadExcelTemplate(columnInfos);
            setExcelResponse(response, "模板.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            log.error("下载模板失败, tableName: {}", tableName, e);
            throw new BusinessException("error.code.fail");
        }
    }


    @PostMapping({"/{tableName}/export", "/admin/{tableName}/export/{appId}"})
    public void export(@PathVariable String tableName,
                       @PathVariable(required = false) String appId,
                       @RequestBody Map<String, Object> params, HttpServletResponse response) {

        String normalizedTableName = normalizeTableName(tableName);
        TableInfoResult tableInfo = getTableInfo(normalizedTableName, appId);

        List<Map<String, Object>> data = fetchExportData(normalizedTableName, tableInfo.getAppId(), params);
        exportDataToFile(tableInfo.getTableInfo().getDescription(), tableInfo.getColumnInfos(), data, response);
    }

    @PostMapping("/{tableName}/excel")
    public void excel(@PathVariable String tableName, @RequestBody Map<String, Object> params, HttpServletResponse response) {
        export(tableName, null, params, response);
    }


    @PostMapping("/{tableName}/import")
    public BaseResponse importExcel(@PathVariable String tableName, @RequestPart(name = "file") MultipartFile file) {
        String normalizedTableName = normalizeTableName(tableName);
        List<AppTableColumnInfo> columnInfos = getTableColumnInfos(normalizedTableName);

        String appId = GlobalAppIdFilter.getAppId();
        excelProvider.importData(file, columnInfos,
                data -> dynamicService.addBatch(appId, normalizedTableName, data));

        return ResultUtils.success();
    }

    /**
     * 获取导出数据
     */
    private List<Map<String, Object>> fetchExportData(String tableName, String appId, Map<String, Object> params) {
        BaseResponse response = dynamicService.list(appId, tableName, params);

        if (!response.isSuccess()) {
            throw new BusinessException("dynamic.list.no_data_export");
        }

        if (ObjectUtils.isEmpty(response.getData())) {
            throw new BusinessException("dynamic.list.no_data_export");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getData();

        // 移除List类型的字段（Excel不支持）
        return data.stream()
                .map(this::removeListFields)
                .collect(Collectors.toList());
    }

    /**
     * 移除Map中的List类型字段
     */
    private Map<String, Object> removeListFields(Map<String, Object> dataMap) {
        return dataMap.entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof List))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * 导出数据到文件
     */
    private void exportDataToFile(String fileName,
                                  List<AppTableColumnInfo> columnInfos,
                                  List<Map<String, Object>> data,
                                  HttpServletResponse response) {
        try {
            List<List<String>> head = buildExcelHead(columnInfos);
            List<List<Object>> dataList = buildExcelData(columnInfos, data);

            String exportFileName = buildExportFileName(fileName);
            setExcelResponse(response, exportFileName);

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("数据")
                    .doWrite(dataList);

        } catch (IOException e) {
            throw new BusinessException("error.code.fail");
        }
    }

    /**
     * 构建Excel表头
     */
    private List<List<String>> buildExcelHead(List<AppTableColumnInfo> columnInfos) {
        return columnInfos.stream()
                .map(column -> Collections.singletonList(column.getColumnComment()))
                .collect(Collectors.toList());
    }

    /**
     * 构建Excel数据
     */
    private List<List<Object>> buildExcelData(List<AppTableColumnInfo> columnInfos,
                                              List<Map<String, Object>> data) {
        return data.stream()
                .map(row -> buildExcelRow(columnInfos, row))
                .collect(Collectors.toList());
    }

    /**
     * 构建Excel单行数据
     */
    private List<Object> buildExcelRow(List<AppTableColumnInfo> columnInfos, Map<String, Object> rowData) {
        List<Object> row = new ArrayList<>();
        for (AppTableColumnInfo column : columnInfos) {
            Object value = rowData.getOrDefault(column.getColumnName(), "");
            if (value instanceof Date) {
                value = DATE_FORMAT.format((Date) value);
            }
            row.add(value);
        }
        return row;
    }

    /**
     * 构建导出文件名
     */
    private String buildExportFileName(String fileName) throws IOException {
        String baseName = StringUtils.isEmpty(fileName) ? "数据" : fileName.trim();
        return URLEncoder.encode(baseName + ".xlsx", StandardCharsets.UTF_8.toString());
    }


    /**
     * 设置Excel响应头
     */
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
    }

    /**
     * 获取表信息和列信息
     */
    private TableInfoResult getTableInfo(String tableName, String appId) {
        if (StringUtils.isEmpty(appId)) {
            appId = GlobalAppIdFilter.getAppId();
        }

        AppTableInfo tableInfo = getTableInfoByTableName(tableName, appId);
        List<AppTableColumnInfo> columnInfos = getColumnInfosByTableId(tableInfo.getId(), appId);

        if (ObjectUtils.isEmpty(columnInfos)) {
            throw new BusinessException("error.code.not_found");
        }

        return new TableInfoResult(tableInfo, columnInfos, appId);
    }

    /**
     * 表信息结果封装类
     */
    private static class TableInfoResult {
        private final AppTableInfo tableInfo;
        private final List<AppTableColumnInfo> columnInfos;
        private final String appId;

        public TableInfoResult(AppTableInfo tableInfo, List<AppTableColumnInfo> columnInfos, String appId) {
            this.tableInfo = tableInfo;
            this.columnInfos = columnInfos;
            this.appId = appId;
        }

        public AppTableInfo getTableInfo() {
            return tableInfo;
        }

        public List<AppTableColumnInfo> getColumnInfos() {
            return columnInfos;
        }

        public String getAppId() {
            return appId;
        }
    }
}
