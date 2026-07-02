package com.minispring.aop.advice;

import java.lang.reflect.Method;

/**
 * 后置增强
 * 在方法执行后调用
 */
@FunctionalInterface
public interface AfterAdvice extends Advice {

    /**
     * 后置处理
     *
     * @param method 目标方法
     * @param args 方法参数
     * @param target 目标对象
     * @param returnValue 方法返回值
     * @param exception 方法抛出的异常（如果有）
     */
    void after(Method method, Object[] args, Object target, Object returnValue, Throwable exception) throws Throwable;
}
