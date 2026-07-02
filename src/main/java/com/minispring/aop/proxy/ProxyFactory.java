package com.minispring.aop.proxy;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.BeforeAdvice;
import com.minispring.aop.advice.AfterAdvice;
import com.minispring.aop.advice.Advice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 代理工厂
 * 创建代理对象并应用增强
 */
public class ProxyFactory {

    private Object target;
    private Class<?>[] interfaces;
    private final List<Advice> advices = new ArrayList<>();

    /**
     * 设置目标对象
     */
    public void setTarget(Object target) {
        this.target = target;
    }

    /**
     * 设置要实现的接口
     */
    public void setInterfaces(Class<?>... interfaces) {
        this.interfaces = interfaces;
    }

    /**
     * 添加增强
     */
    public void addAdvice(Advice advice) {
        if (advice != null) {
            advices.add(advice);
        }
    }

    /**
     * 创建代理对象
     */
    public Object getProxy() {
        if (interfaces != null && interfaces.length > 0) {
            // 使用JDK动态代理
            return createJdkProxy();
        } else {
            // 使用CGLIB代理
            return createCglibProxy();
        }
    }

    /**
     * 创建JDK动态代理
     */
    private Object createJdkProxy() {
        return Proxy.newProxyInstance(
            getClass().getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
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
                        return method.invoke(target, args);
                    }
                };

                return applyAdvices(invocation);
            }
        );
    }

    /**
     * 创建CGLIB代理
     */
    private Object createCglibProxy() {
        // 简化实现：使用JDK代理处理接口情况
        // 真正的CGLIB需要第三方库，这里使用简化版本
        return new CglibProxy(target).createProxy(advices);
    }

    /**
     * 应用所有增强
     */
    private Object applyAdvices(MethodInvocation invocation) throws Throwable {
        // 创建责任链
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
