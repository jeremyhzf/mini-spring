package com.minispring.aop;

/**
 * 类过滤器
 * 判断类是否匹配
 */
@FunctionalInterface
public interface ClassFilter {

    /**
     * 匹配所有类的过滤器
     */
    ClassFilter TRUE = clazz -> true;

    /**
     * 判断类是否匹配
     *
     * @param clazz 目标类
     * @return 是否匹配
     */
    boolean matches(Class<?> clazz);
}
