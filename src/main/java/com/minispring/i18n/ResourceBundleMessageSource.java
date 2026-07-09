package com.minispring.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 基于 classpath .properties 的消息源
 * basename 可配（如 "messages" 对应 messages.properties / messages_en.properties / messages_zh.properties）。
 * locale 回退复用 ResourceBundle.getBundle 原生策略（请求 locale → JVM 默认 → 根 bundle）。
 */
public class ResourceBundleMessageSource extends AbstractMessageSource {

    private String basename;

    /**
     * 设置资源 bundle 基名
     *
     * @param basename 基名（不含语言后缀与 .properties）
     */
    public void setBasename(String basename) {
        this.basename = basename;
    }

    @Override
    protected String resolveCode(String code, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(basename, locale);
            return bundle.containsKey(code) ? bundle.getString(code) : null;
        } catch (MissingResourceException e) {
            // basename 对应 bundle 完全不存在 → 视为找不到
            return null;
        }
    }
}
