package com.minispring.aop.interceptor;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

/**
 * 性能监控拦截器
 * 监控方法执行时间
 */
public class PerformanceMonitorInterceptor implements AroundAdvice {

    private long warnThreshold = 1000;

    public void setWarnThreshold(long warnThreshold) {
        this.warnThreshold = warnThreshold;
    }

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > warnThreshold) {
                System.out.println("[性能警告] 方法 " + invocation.getMethod().getName() +
                                   " 执行时间: " + elapsed + "ms");
            }
        }
    }
}
