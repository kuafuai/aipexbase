package com.kuafuai.common.exception;


import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.kuafuai.common.config.MessageConfig;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * 全局异常处理器
 *
 * @author kuafui
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Pattern TABLE_NOT_EXIST_PATTERN = Pattern.compile(":(\\w+):数据不存在");


    @Resource
    private MessageConfig messageConfig;

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    public BaseResponse<?> rateLimitExceptionHandler(RateLimitException e) {
        // 可以添加监控指标
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<?> handleMissingParams(MissingServletRequestParameterException ex) {
        String message = I18nUtils.get("error.param.required", ex.getParameterName());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("===={}====", e.getMessage());
        Throwable ex = e;
        while (ex != null) {
            log.warn("Cause: {}", ex.getClass().getName());
            ex = ex.getCause();
        }
        // 其他异常才发送消息
        sendMessage(e.getMessage());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统开小差拉。可以联系客服，帮你查看问题哦！");
    }


    /**
     * 用户不存在
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public BaseResponse<?> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, I18nUtils.get("login.login.user_not_find"));
    }

    /**
     * 密码不对
     */
    @ExceptionHandler(BadCredentialsException.class)
    public BaseResponse<?> handleBadCredentialsException(BadCredentialsException e) {
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, I18nUtils.get("login.login.password"));
    }

    @ExceptionHandler(MybatisPlusException.class)
    public BaseResponse<?> handleMybatisPlusException(MybatisPlusException e) {
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, I18nUtils.get("error.data.mybatis.error", e.getMessage()));
    }

    /**
     * Mybatis
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public BaseResponse<?> handleMyBatisException(MyBatisSystemException e) {
        BusinessException bizEx = (BusinessException) getCauseByType(e, BusinessException.class);
        if (bizEx != null) {
            sendMessage(bizEx.getMessage());
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, bizEx.getMessage());
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {

        return ResultUtils.error("error.data.method.not_support");
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<?> handleBindException(BindException e) {
        ObjectError error = e.getAllErrors().get(0);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        if (error instanceof FieldError) {
            String filed = ((FieldError) error).getField();
            message = filed + ":" + message;
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    public static Throwable getCauseByType(Throwable e, Class<? extends Throwable> targetType) {
        while (e != null) {
            if (targetType.isInstance(e)) {
                return e;
            }
            e = e.getCause();
        }
        return null;
    }

    private void sendMessage(String message) {
        String appId = GlobalAppIdFilter.getAppId();
        String textMessage = "";
        if (StringUtils.isNotEmpty(appId)) {
            textMessage = "APP_ID: " + appId + "\n" + message;
        } else {
            textMessage = message;
        }

        final HashMap<String, Object> body = new HashMap<>();
        body.put("msg_type", "text");
        final HashMap<String, Object> contentMap = new HashMap<>();
        contentMap.put("text", textMessage);

        body.put("content", contentMap);
        HttpUtil.post(messageConfig.getNotifyUrl(), JSON.toJSONString(body));
    }
}
