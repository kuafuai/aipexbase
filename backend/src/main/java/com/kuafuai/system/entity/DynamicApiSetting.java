package com.kuafuai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "dynamic_api_setting")
public class DynamicApiSetting {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String appId;

    private String keyName;
    private String description;

    private String url;
    private String token;

    private String method;
    private String bodyType;
    private String bodyTemplate;
    private String header;

    private String protocol;
    private String dataPath;
    private String dataRaw;
    private String dataType;
    @TableField(value = "`show`")
    private Integer show;
    private String varRaw;

    private Integer marketId;
}
