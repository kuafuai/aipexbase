package com.kuafuai.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("api_key")
public class APIKey {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String appId;

    private String name;

    private String keyName;

    private String description;

    private String status;

    private String createAt;

    private String lastUsedAt;

    private String expireAt;

    public enum APIKeyStatus {
        ACTIVE,
        DISABLE
    }
}
