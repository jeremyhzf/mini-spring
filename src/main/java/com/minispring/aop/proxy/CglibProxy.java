package com.minispring.aop.proxy;

import com.minispring.aop.advice.Advice;
import com.minispring.aop.advice.MethodInvocation;
import com.minispring.aop.advice.AroundAdvice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * 简化的CGLIB代理实现
 * 实际上使用JDK代理作为简化实现
 */
public class CglibProxy implements InvocationHandler {

    private final Object target;
    private List<Advice> advices;

    public CglibProxy(Object target) {
        this.target = target;
    }

    public Object createProxy(List<Advice> advices) {
        this.advices = advices;

        // 获取目标类的所有接口
        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length == 0) {
            // 如果没有接口，添加一个标记接口
            interfaces = new Class<?>[] { ProxyMarker.class };
        }

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

    private Object applyAdvices(MethodInvocation invocation) throws Throwable {
        MethodInvocation chain = invocation;

        for (int i = advices.size() - 1; i >= 0; i--) {
            Advice advice = advices.get(i);
            MethodInvocation next = chain;

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

    /**
     * 代理标记接口
     */
    public interface ProxyMarker {}
}
