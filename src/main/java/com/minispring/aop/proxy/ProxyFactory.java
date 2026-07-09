package com.minispring.aop.proxy;

import com.minispring.aop.Advisor;
import com.minispring.aop.Pointcut;
import com.minispring.aop.ClassFilter;
import com.minispring.aop.MethodMatcher;
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
    private final List<Advisor> advisors = new ArrayList<>();

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
     * 添加通知器
     */
    public void addAdvisor(Advisor advisor) {
        if (advisor != null) {
            advisors.add(advisor);
        }
    }

    /**
     * 检查是否有Advisor
     */
    public boolean hasAdvisors() {
        return !advisors.isEmpty();
    }

    /**
     * 获取所有Advisors
     */
    public List<Advisor> getAdvisors() {
        return new ArrayList<>(advisors);
    }

    /**
     * 添加多个Advisors
     */
    public void addAdvisors(List<Advisor> advisors) {
        if (advisors != null) {
            this.advisors.addAll(advisors);
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
            target.getClass().getClassLoader(),
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
                        try {
                            return method.invoke(target, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            throw e.getCause();
                        }
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
     * 执行顺序：BeforeAdvice -> AroundAdvice -> 目标方法 -> AfterAdvice
     */
    private Object applyAdvices(MethodInvocation invocation) throws Throwable {
        // 1. 执行所有BeforeAdvice
        for (Advice advice : advices) {
            if (advice instanceof BeforeAdvice) {
                ((BeforeAdvice) advice).before(
                    invocation.getMethod(),
                    invocation.getArguments(),
                    invocation.getTarget()
                );
            }
        }

        // 2. 构建AroundAdvice责任链
        MethodInvocation chain = invocation;
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getTarget().getClass();

        // 首先处理直接添加的Advice
        for (int i = advices.size() - 1; i >= 0; i--) {
            final Advice advice = advices.get(i);
            final MethodInvocation next = chain;

            if (advice instanceof AroundAdvice) {
                chain = new ChainInvocation(next, (AroundAdvice) advice);
            }
        }

        // 然后处理Advisor（只应用匹配的增强）
        for (int i = advisors.size() - 1; i >= 0; i--) {
            Advisor advisor = advisors.get(i);
            Pointcut pointcut = advisor.getPointcut();

            // 检查是否匹配
            if (pointcut.getClassFilter().matches(targetClass) &&
                pointcut.getMethodMatcher().matches(method, targetClass)) {

                Advice advice = advisor.getAdvice();
                MethodInvocation next = chain;

                if (advice instanceof AroundAdvice) {
                    chain = new ChainInvocation(next, (AroundAdvice) advice);
                }
            }
        }

        // 3. 执行责任链（包含所有AroundAdvice和目标方法）
        Object returnValue = null;
        Throwable exception = null;
        try {
            returnValue = chain.proceed();
            return returnValue;
        } catch (Throwable t) {
            exception = t;
            if (t instanceof java.lang.reflect.InvocationTargetException) {
                exception = t.getCause();  // 解包原始异常给AfterAdvice使用
                throw exception;  // 抛出原始异常
            }
            throw t;
        } finally {
            // 4. 执行所有AfterAdvice（无论成功还是异常）
            for (Advice advice : advices) {
                if (advice instanceof AfterAdvice) {
                    ((AfterAdvice) advice).after(
                        invocation.getMethod(),
                        invocation.getArguments(),
                        invocation.getTarget(),
                        returnValue,
                        exception
                    );
                }
            }
        }
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
