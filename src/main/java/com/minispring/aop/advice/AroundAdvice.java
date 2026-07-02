package com.minispring.aop.advice;

/**
 * 环绕增强
 * 拦截方法执行，可以控制是否调用目标方法
 */
@FunctionalInterface
public interface AroundAdvice extends Advice {

    /**
     * 环绕处理
     *
     * @param invocation 方法调用信息
     * @return 方法返回值
     */
    Object around(MethodInvocation invocation) throws Throwable;
}
