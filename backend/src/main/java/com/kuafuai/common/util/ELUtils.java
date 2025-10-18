package com.kuafuai.common.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class ELUtils {

    private static final ExpressionParser parser = new SpelExpressionParser();

    public static String parseExpression(String expression, ProceedingJoinPoint joinPoint) {
        if (expression.contains("#") || expression.contains("T(")) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 创建解析上下文并设置方法参数
            StandardEvaluationContext context = new StandardEvaluationContext();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // 解析 SpEL 表达式
            return parser.parseExpression(expression).getValue(context, String.class);
        } else {
            // 如果是普通字符串，直接返回
            return expression;
        }
    }
}
