package com.kuafuai.common.dynamic_config.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * 
 * @TableName system_config
 */
@TableName(value ="system_config")
@Data
@ToString
@EqualsAndHashCode
public class SystemConfig implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String chineseName;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private String content;

    /**
     * 
     */
    private String remark;

    /**
     * 
     */
    private String type;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

   }