package com.kuafuai.manage.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DataVo {
    private String appId;
    private String tableName;
    private Map<String, Object> data;
    private List<Map<String, Object>> dataList;

    /**
     * 批量导入配置，格式：{@code {"fieldMapping":{...},"extraFields":{...}}}
     * 子键支持 snake_case：field_mapping、extra_fields。
     * fieldMapping：源字段名 -&gt; 目标字段名；extraFields：目标列名 -&gt; 默认值（映射后按行合并，缺 key 或值为 null 才填）。
     */
    @JsonProperty("importConfig")
    @JsonAlias({"import_config", "import-config"})
    @JsonDeserialize(using = ImportConfigDeserializer.class)
    private Map<String, Object> importConfig;

    /**
     * 从 {@link #importConfig} 解析出批量写入所需的映射与补充字段；二者均可为 null。
     */
    public static ImportBatchOptions resolveImportConfig(Map<String, Object> importConfig) {
        if (importConfig == null || importConfig.isEmpty()) {
            return new ImportBatchOptions(null, null);
        }
        Object fm = importConfig.get("fieldMapping");
        if (fm == null) {
            fm = importConfig.get("field_mapping");
        }
        Object ef = importConfig.get("extraFields");
        if (ef == null) {
            ef = importConfig.get("extra_fields");
        }

        Map<String, String> fieldMapping = null;
        if (fm instanceof Map) {
            fieldMapping = coerceStringStringMap((Map<?, ?>) fm);
        }
        Map<String, Object> extraFields = null;
        if (ef instanceof Map) {
            extraFields = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) ef).entrySet()) {
                extraFields.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        return new ImportBatchOptions(fieldMapping, extraFields);
    }

    private static Map<String, String> coerceStringStringMap(Map<?, ?> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            out.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        }
        return out;
    }

    public static final class ImportBatchOptions {
        private final Map<String, String> fieldMapping;
        private final Map<String, Object> extraFields;

        private ImportBatchOptions(Map<String, String> fieldMapping, Map<String, Object> extraFields) {
            this.fieldMapping = fieldMapping;
            this.extraFields = extraFields;
        }

        public Map<String, String> getFieldMapping() {
            return fieldMapping;
        }

        public Map<String, Object> getExtraFields() {
            return extraFields;
        }
    }
}
