package com.kuafuai.system.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据库完整结构信息 VO
 * 包含数据库中所有表及其字段信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseStructureVO {
    
    /**
     * 数据库名称
     */
    private String databaseName;
    
    /**
     * 表列表
     */
    private List<TableStructureVO> tables;
    
    /**
     * 表结构信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableStructureVO {
        
        /**
         * 表名
         */
        private String tableName;
        
        /**
         * 表注释
         */
        private String tableComment;
        
        /**
         * 字段列表
         */
        private List<ColumnStructureVO> columns;
    }
    
    /**
     * 字段结构信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnStructureVO {
        
        /**
         * 字段名
         */
        private String columnName;
        
        /**
         * 字段类型
         */
        private String columnType;
        
        /**
         * 是否允许为空
         */
        private String nullable;
        
        /**
         * 键类型（PRI主键，UNI唯一键，MUL普通索引等）
         */
        private String colKey;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        /**
         * 字段注释
         */
        private String comment;
    }
}
