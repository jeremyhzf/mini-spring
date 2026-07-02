package com.minispring.aop.advice;

import java.lang.reflect.Method;

/**
 * 前置增强
 * 在方法执行前调用
 */
@FunctionalInterface
public interface BeforeAdvice extends Advice {

    /**
     * 前置处理
     *
     * @param method 目标方法
     * @param args 方法参数
     * @param target 目标对象
     */
    void before(Method method, Object[] args, Object target) throws Throwable;
}
