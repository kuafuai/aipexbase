package com.kuafuai.common.domin;

/**
 * 返回工具类
 *
 * @author kuafui
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static BaseResponse success() {
        return new BaseResponse<>(0, null, "ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static BaseResponse error() {
        return new BaseResponse(-1, null, "system error");
    }

    public static BaseResponse error(String message) {
        return new BaseResponse(-1, null, message);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @return
     */
    public static BaseResponse error(int code, String message) {
        return new BaseResponse(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode, String message) {
        return new BaseResponse(errorCode.getCode(), null, message);
    }

    public static BaseResponse success(Object o, String message) {
        return new BaseResponse(0, o, message);
    }
}
