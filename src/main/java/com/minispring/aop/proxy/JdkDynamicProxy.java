package com.minispring.aop.proxy;

import com.minispring.aop.advice.Advice;
import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK动态代理实现
 * 基于Java反射机制实现代理
 */
public class JdkDynamicProxy implements InvocationHandler {

    private final Object target;
    private List<Advice> advices;

    public JdkDynamicProxy(Object target) {
        this.target = target;
    }

    /**
     * 创建代理对象
     */
    public Object createProxy(Class<?>[] interfaces, List<Advice> advices) {
        this.advices = advices;
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            interfaces,
            this
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation invocation = new MethodInvocation() {
            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Object[] getArguments() {
                return args;
            }

            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Object proceed() throws Throwable {
                try {
                    return method.invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };

        return applyAdvices(invocation);
    }

    /**
     * 应用所有增强
     */
    private Object applyAdvices(MethodInvocation invocation) throws Throwable {
        MethodInvocation chain = invocation;

        // 反向遍历，构建责任链
        for (int i = advices.size() - 1; i >= 0; i--) {
            final Advice advice = advices.get(i);
            final MethodInvocation next = chain;

            if (advice instanceof AroundAdvice) {
                chain = new ChainInvocation(next, (AroundAdvice) advice);
            }
        }

        return chain.proceed();
    }

    /**
     * 责任链调用实现
     */
    private static class ChainInvocation implements MethodInvocation {
        private final MethodInvocation next;
        private final AroundAdvice advice;

        public ChainInvocation(MethodInvocation next, AroundAdvice advice) {
            this.next = next;
            this.advice = advice;
        }

        @Override
        public Method getMethod() {
            return next.getMethod();
        }

        @Override
        public Object[] getArguments() {
            return next.getArguments();
        }

        @Override
        public Object getTarget() {
            return next.getTarget();
        }

        @Override
        public Object proceed() throws Throwable {
            return advice.around(next);
        }
    }
}
