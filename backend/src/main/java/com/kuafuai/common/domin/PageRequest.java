package com.kuafuai.common.domin;

import lombok.Data;

/**
 * 分页请求
 *
 * @author kuafui
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

}
