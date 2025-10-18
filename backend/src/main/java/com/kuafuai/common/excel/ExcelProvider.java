package com.kuafuai.common.excel;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.AppTableColumnInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ExcelProvider {

    private static final int BATCH_COUNT = 100;
    private static final String TEMPLATE_SHEET_NAME = "模板";

    // 数据库类型常量
    private static final String TYPE_VARCHAR = "varchar";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_INT = "int";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_DECIMAL = "DECIMAL";
    private static final String TYPE_BOOL = "BOOL";
    private static final String TYPE_TIME = "TIME";
    private static final String TYPE_DATETIME = "DATETIME";


    public Workbook downloadExcelTemplate(List<AppTableColumnInfo> columnInfos) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(TEMPLATE_SHEET_NAME);

        List<AppTableColumnInfo> filteredColumns = filterAutoIncrementPrimaryKey(columnInfos);
        createTableHeader(workbook, sheet, filteredColumns);

        createExampleDataRow(workbook, sheet, filteredColumns);

        autoSizeColumns(sheet, filteredColumns.size());
        return workbook;
    }

    private List<AppTableColumnInfo> filterAutoIncrementPrimaryKey(List<AppTableColumnInfo> columnInfos) {
        return columnInfos.stream()
                .filter(column -> !(column.isPrimary() && StringUtils.equalsIgnoreCase(column.getColumnType(), TYPE_INT)))
                .collect(Collectors.toList());
    }

    /**
     * 创建表头
     */
    private void createTableHeader(Workbook workbook, Sheet sheet, List<AppTableColumnInfo> columns) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < columns.size(); i++) {
            AppTableColumnInfo column = columns.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(column.getColumnComment());
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());

        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private void createExampleDataRow(Workbook workbook, Sheet sheet, List<AppTableColumnInfo> columns) {
        Row dataRow = sheet.createRow(1);

        for (int i = 0; i < columns.size(); i++) {
            AppTableColumnInfo column = columns.get(i);
            Cell cell = dataRow.createCell(i);
            setExampleCellValue(workbook, cell, column);
        }
    }

    /**
     * 设置示例单元格值
     */
    private void setExampleCellValue(Workbook workbook, Cell cell, AppTableColumnInfo columnInfo) {
        String columnType = columnInfo.getColumnType().toLowerCase();

        if (StringUtils.containsAnyIgnoreCase(columnType, TYPE_VARCHAR, TYPE_TEXT)) {
            cell.setCellValue("示例文本");
        } else if (StringUtils.containsAnyIgnoreCase(columnType, TYPE_INT)) {
            cell.setCellValue(1);
        } else if (StringUtils.containsAnyIgnoreCase(columnType, TYPE_FLOAT, TYPE_DECIMAL)) {
            cell.setCellValue(1.0);
        } else if (StringUtils.containsAnyIgnoreCase(columnType, TYPE_BOOL)) {
            cell.setCellValue(true);
        } else if (StringUtils.equalsIgnoreCase(columnType, TYPE_TIME)) {
            cell.setCellValue(new Date());
            CellStyle dateStyle = createDateStyle(workbook, "HH:mm:ss");
            cell.setCellStyle(dateStyle);
        } else if (StringUtils.equalsIgnoreCase(columnType, TYPE_DATETIME)) {
            cell.setCellValue(new Date());
            CellStyle dateStyle = createDateStyle(workbook, "yyyy-MM-dd HH:mm:ss");
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue("其他类型");
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static CellStyle createDateStyle(Workbook workbook, String parttern) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat(parttern));
        return style;
    }

    /**
     * 执行数据导入
     */
    public void importData(MultipartFile file, List<AppTableColumnInfo> columnInfos, Consumer<List<Map<String, Object>>>
            saveFunction) {

        // 验证文件
        validateFile(file);

        // 过滤主键字段
        List<AppTableColumnInfo> filteredColumns = columnInfos.stream()
                .filter(column -> !column.isPrimary())
                .collect(Collectors.toList());
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, new ExcelDataListener(filteredColumns, saveFunction))
                    .sheet()
                    .doRead();
        } catch (ExcelDataConvertException e) {
            log.error("Excel数据转换失败，行: {}, 列: {}", e.getRowIndex(), e.getColumnIndex(), e);
            throw new BusinessException("请检查Excel数据格式，确保数据类型与模板一致");
        } catch (IOException e) {
            log.error("读取Excel文件失败", e);
            throw new BusinessException("读取文件失败，请检查文件是否正确");
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new BusinessException("只支持Excel文件(.xlsx, .xls)");
        }
    }

    private static class ExcelDataListener implements ReadListener<Map<Integer, Object>> {
        private final List<AppTableColumnInfo> columnInfos;
        private final Consumer<List<Map<String, Object>>> saveFunction;
        private List<Map<String, Object>> cachedDataList;
        private int totalCount = 0;

        public ExcelDataListener(List<AppTableColumnInfo> columnInfos,
                                 Consumer<List<Map<String, Object>>> saveFunction) {
            this.columnInfos = columnInfos;
            this.saveFunction = saveFunction;
            this.cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }

        @Override
        public void invoke(Map<Integer, Object> rowData, AnalysisContext analysisContext) {
            Map<String, Object> convertedData = convertRowData(rowData);
            if (log.isDebugEnabled()) {
                log.debug("解析到第{}条数据: {}", totalCount + 1, convertedData);
            }

            cachedDataList.add(convertedData);
            totalCount++;

            if (cachedDataList.size() >= BATCH_COUNT) {
                saveData();
                cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
            saveData();
            log.info("Excel导入完成，共处理{}条数据", totalCount);
        }

        /**
         * 转换行数据
         */
        private Map<String, Object> convertRowData(Map<Integer, Object> rowData) {
            Map<String, Object> convertedData = new HashMap<>();

            for (int i = 0; i < columnInfos.size(); i++) {
                if (i < rowData.size()) {
                    AppTableColumnInfo columnInfo = columnInfos.get(i);
                    convertedData.put(columnInfo.getColumnName(), rowData.get(i));
                }
            }

            return convertedData;
        }

        /**
         * 保存数据
         */
        private void saveData() {
            if (cachedDataList.isEmpty()) {
                return;
            }

            try {
                saveFunction.accept(cachedDataList);
                log.info("成功保存{}条数据", cachedDataList.size());
            } catch (Exception e) {
                log.error("保存数据失败", e);
                throw new BusinessException("数据保存失败: " + e.getMessage());
            }
        }
    }

}

