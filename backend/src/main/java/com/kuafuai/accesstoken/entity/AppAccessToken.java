package com.kuafuai.accesstoken.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("app_access_token")
public class AppAccessToken {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String appId;

    private String token;

    /**
     * 允许访问的接口路径，JSON 数组，支持 AntPath 通配符。
     * null 或空表示放行所有接口。
     * 示例：["/api/data/invoke", "/api/data/page/**"]
     */
    private String allowedPaths;

    private Date createdAt;

    private Date updatedAt;
}
