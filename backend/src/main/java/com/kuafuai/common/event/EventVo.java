package com.kuafuai.common.event;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class EventVo<T> {

    private String appId;
    
    /**
     * add/update
     */
    private String model;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 数据
     */

    private T data;
}
