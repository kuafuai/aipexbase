package com.kuafuai.common.exception;


import cn.hutool.http.HttpUtil;
import com.kuafuai.common.config.MessageConfig;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.regex.Matcher;
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<?> handleMissingParams(MissingServletRequestParameterException ex) {
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "参数：(" + ex.getParameterName() + ")不能为空");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);

        if (StringUtils.containsIgnoreCase(e.getMessage(), "query result is multiple records")) {

            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "数据操作异常");
        } else if (StringUtils.equalsIgnoreCase(e.getMessage(), "密码有误")) {

            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "密码错误");
        } else if (StringUtils.equalsIgnoreCase(e.getMessage(), "用户不存在")) {

            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "用户名不存在");
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "值不能为空")) {

            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "表不存在")) {
            sendMessage(e.getMessage());
            String msg = e.getMessage();
            Matcher matcher = TABLE_NOT_EXIST_PATTERN.matcher(msg);
            if (matcher.find()) {
                msg = matcher.group(1) + ":表不存在";
            }
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, msg);
        }

        // 其他异常才发送消息
        sendMessage(e.getMessage());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统开小差拉。可以联系客服，帮你查看问题哦！");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
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
