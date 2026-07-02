package com.minispring.aop.interceptor;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.Method;

/**
 * 日志拦截器
 * 记录方法调用的日志信息
 */
public class LoggingInterceptor implements AroundAdvice {

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();

        System.out.println("[日志] 调用方法: " + method.getName());
        if (args.length > 0) {
            System.out.println("[日志] 参数: " + java.util.Arrays.toString(args));
        }

        long startTime = System.currentTimeMillis();
        try {
            Object result = invocation.proceed();
            System.out.println("[日志] 返回值: " + result);
            System.out.println("[日志] 耗时: " + (System.currentTimeMillis() - startTime) + "ms");
            return result;
        } catch (Exception e) {
            System.out.println("[日志] 异常: " + e.getMessage());
            throw e;
        }
    }
}
