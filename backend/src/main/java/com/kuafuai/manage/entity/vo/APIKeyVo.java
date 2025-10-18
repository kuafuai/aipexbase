package com.kuafuai.manage.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIKeyVo {

    private String name;

    private String description;

    private String expireAt;
}
