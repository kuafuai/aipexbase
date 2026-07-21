package com.kuafuai.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageBreakdownVO {
    /** image / video / audio / doc / other */
    private String kind;
    private Long count;
    private Long bytes;
}
