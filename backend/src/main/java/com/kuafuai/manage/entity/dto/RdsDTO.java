package com.kuafuai.manage.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RdsDTO {
    private String url;
    private String username;
    private String password;
    private Double priority;
    private String rdsKey;
}
