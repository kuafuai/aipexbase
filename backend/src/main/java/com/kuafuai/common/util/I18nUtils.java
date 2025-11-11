package com.kuafuai.common.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nUtils {

    private static MessageSource messageSource;

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        I18nUtils.messageSource = messageSource;
    }

    /**
     * 获取国际化文案（无参数）
     */
    public static String get(String code) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, locale);
    }

    /**
     * 获取国际化文案（带参数）
     */
    public static String get(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }

    public static String getOrDefault(String code, String defaultMsg) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, defaultMsg, locale);
    }

    /**
     * 获取国际化文案（带默认值）
     */
    public static String getOrDefault(String code, String defaultMsg, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMsg, locale);
    }

    /**
     * 手动指定 Locale
     */
    public static String getByLocale(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

}
