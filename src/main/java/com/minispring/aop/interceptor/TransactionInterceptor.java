package com.minispring.aop.interceptor;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

/**
 * 事务拦截器
 * 管理事务边界
 */
public class TransactionInterceptor implements AroundAdvice {

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        // 开始事务
        System.out.println("[事务] 开启事务");

        try {
            Object result = invocation.proceed();
            // 提交事务
            System.out.println("[事务] 提交事务");
            return result;
        } catch (Exception e) {
            // 回滚事务
            System.out.println("[事务] 回滚事务");
            throw e;
        }
    }
}
