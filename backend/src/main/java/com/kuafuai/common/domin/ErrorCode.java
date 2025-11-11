package com.kuafuai.common.domin;

import com.kuafuai.common.util.I18nUtils;

/**
 * 错误码
 *
 * @author kuafui
 */
public enum ErrorCode {

    SUCCESS(0, "error.code.success"),
    PARAMS_ERROR(40000, "error.code.params_error"),
    NOT_LOGIN_ERROR(40100, "error.code.not_login"),
    NO_AUTH_ERROR(40101, "error.code.no_auth"),
    NOT_GET_LOCK(40010, "error.code.not_get_lock"),
    NOT_FOUND_ERROR(40400, "error.code.not_found"),
    NOT_BIND_DATA_ERROR(40401, "error.code.not_bind_data"),
    FORBIDDEN_ERROR(40300, "error.code.forbidden"),
    SYSTEM_ERROR(50000, "error.code.system_error"),
    OPERATION_ERROR(50001, "error.code.operation_error"),
    BALANCE_NOT_ENOUGH(50002, "error.code.balance_not_enough")
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String messageKey;

    ErrorCode(int code, String message) {
        this.code = code;
        this.messageKey = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getMessage() {
        return I18nUtils.get(this.messageKey);
    }

    public String getMessage(Object... args) {
        return I18nUtils.get(this.messageKey, args);
    }

}
