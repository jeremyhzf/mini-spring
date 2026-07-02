package com.minispring.aop;

import java.lang.reflect.Method;

/**
 * 方法匹配器
 * 判断方法是否匹配切点
 */
@FunctionalInterface
public interface MethodMatcher extends Pointcut {

    /**
     * 判断方法是否匹配
     *
     * @param method 目标方法
     * @param targetClass 目标类
     * @return 是否匹配
     */
    boolean matches(Method method, Class<?> targetClass);

    @Override
    default ClassFilter getClassFilter() {
        return ClassFilter.TRUE;
    }

    @Override
    default MethodMatcher getMethodMatcher() {
        return this;
    }
}
