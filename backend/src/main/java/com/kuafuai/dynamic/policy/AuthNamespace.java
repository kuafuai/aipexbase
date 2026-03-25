package com.kuafuai.dynamic.policy;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Slf4j
public class AuthNamespace implements FunctionNamespace {

    private final Map<String, Function<LoginUser, RlsValue>> methods = new HashMap<>();

    public AuthNamespace() {
        // 注册字符串类型字段
        registerStringGetter("uid", LoginUser::getRelevanceId);
        registerStringGetter("app_id", LoginUser::getAppId);

        log.info("AuthNamespace 初始化完成，注册方法数: {}", methods.size());
    }

    @Override
    public String getName() {
        return "auth";
    }

    @Override
    public RlsValue evaluate(String methodName) {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        // 未登录：返回 NULL（正常情况，不记录警告）
        if (loginUser == null) {
            return RlsValue.ofNull();
        }

        // 从注册表中查找求值函数
        Function<LoginUser, RlsValue> evaluator = methods.get(methodName);

        if (evaluator == null) {
            log.warn("未知的 auth 方法: {}，支持的方法: {}", methodName, methods.keySet());
            return RlsValue.ofNull();
        }

        // 执行求值
        return evaluator.apply(loginUser);
    }

    public void registerMethod(String methodName, Function<LoginUser, RlsValue> evaluator) {
        if (methods.containsKey(methodName)) {
            log.warn("auth 方法 {} 已存在，将被覆盖", methodName);
        }
        methods.put(methodName, evaluator);
    }

    private void registerStringGetter(String methodName, Function<LoginUser, String> getter) {
        registerMethod(methodName, user -> {
            String value = getter.apply(user);
            return StringUtils.isEmpty(value) ? RlsValue.ofNull() : RlsValue.ofString(value);
        });
    }

    private void registerNumberGetter(String methodName, Function<LoginUser, ? extends Number> getter) {
        registerMethod(methodName, user -> {
            Number value = getter.apply(user);
            return value == null ? RlsValue.ofNull() : RlsValue.ofNumber(value);
        });
    }

    private void registerBooleanGetter(String methodName, Function<LoginUser, Boolean> getter) {
        registerMethod(methodName, user -> {
            Boolean value = getter.apply(user);
            return value == null ? RlsValue.ofNull() : RlsValue.ofBoolean(value);
        });
    }
}
