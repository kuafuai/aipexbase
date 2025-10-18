package com.kuafuai.api.parser;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;

public class ApiResultParser {
    /**
     * 解析从三方api返回的调用结果，如果失败，那么，直接抛出异常进行返回
     * @param result
     */
    public static void parser(String result){
        if (StringUtils.isNotEmpty(result)) {
            final BaseResponse baseResponse = JSON.parseObject(result, BaseResponse.class);
            if (baseResponse.getCode() != ErrorCode.SUCCESS.getCode()) {
                throw new BusinessException(baseResponse.getCode(), baseResponse.getMessage());
            }
        }
    }
}
