package com.minispring.env;

/**
 * 环境抽象
 * 用于解析属性值
 */
public interface Environment {

    /**
     * 获取属性值
     */
    String getProperty(String key);

    /**
     * 获取属性值，带默认值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 设置属性值
     */
    void setProperty(String key, String value);

    /**
     * 解析占位符
     * 例如: ${app.name} -> 实际值
     */
    String resolvePlaceholders(String text);
}
