package com.minispring.aop.advice;

import java.lang.reflect.Method;

/**
 * 方法调用上下文
 */
public interface MethodInvocation {

    /**
     * 获取目标方法
     */
    Method getMethod();

    /**
     * 获取方法参数
     */
    Object[] getArguments();

    /**
     * 获取目标对象
     */
    Object getTarget();

    /**
     * 执行目标方法
     */
    Object proceed() throws Throwable;
}
