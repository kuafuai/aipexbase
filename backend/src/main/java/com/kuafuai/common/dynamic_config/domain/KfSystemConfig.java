package com.kuafuai.common.dynamic_config.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * 
 * @TableName system_config
 */
@TableName(value ="kf_system_config")
@Data
@ToString
@EqualsAndHashCode
public class KfSystemConfig extends SystemConfig implements Serializable {

}