package com.kuafuai.common.exception;

import com.kuafuai.common.constant.HttpStatus;

public class RateLimitException extends BusinessException{
    public RateLimitException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "error.rate_limit.too_many_requests");
    }
}
