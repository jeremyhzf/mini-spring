package com.minispring.factory;

import com.minispring.aop.Advisor;
import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.aop.interceptor.LoggingInterceptor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * AOP与容器集成测试
 */
public class AopContainerIntegrationTest {

    @Test
    void shouldApplyAopProxy() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        // 添加日志拦截器，匹配所有方法
        container.addAdvisor(new DefaultAdvisor(
            new LoggingInterceptor(),
            (MethodMatcher) (method, targetClass) -> true
        ));

        container.registerBean("service", AopTestServiceImpl.class);

        AopTestService proxy = (AopTestService) container.getBean("service");

        // 验证代理对象不是原始类
        assertFalse(proxy.getClass().equals(AopTestServiceImpl.class),
            "Proxy should not be the original implementation class");

        // 验证方法可以被调用
        assertDoesNotThrow(() -> proxy.execute("test"));
    }

    @Test
    void shouldApplyLoggingInterceptor() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        // 添加日志拦截器
        container.addAdvisor(new DefaultAdvisor(
            new LoggingInterceptor(),
            (MethodMatcher) (method, targetClass) -> true
        ));

        container.registerBean("service", AopTestServiceImpl.class);

        AopTestService proxy = (AopTestService) container.getBean("service");

        // 捕获System.out输出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            proxy.execute("testParam");

            String output = outContent.toString();
            assertTrue(output.contains("[日志] 调用方法: execute"),
                "Should log method invocation");
            assertTrue(output.contains("[日志] 参数: [testParam]"),
                "Should log method parameters");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void shouldReturnSameProxyInstanceForSingleton() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        container.addAdvisor(new DefaultAdvisor(
            new LoggingInterceptor(),
            (MethodMatcher) (method, targetClass) -> true
        ));

        container.registerBean("service", AopTestServiceImpl.class);

        AopTestService proxy1 = (AopTestService) container.getBean("service");
        AopTestService proxy2 = (AopTestService) container.getBean("service");

        assertSame(proxy1, proxy2, "Should return same proxy instance for singleton");
    }

    @Test
    void shouldWorkWithMultipleAdvisors() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        // 添加多个Advisor
        container.addAdvisor(new DefaultAdvisor(
            new LoggingInterceptor(),
            (MethodMatcher) (method, targetClass) -> true
        ));

        container.addAdvisor(new DefaultAdvisor(
            new com.minispring.aop.advice.AroundAdvice() {
                @Override
                public Object around(com.minispring.aop.advice.MethodInvocation invocation) throws Throwable {
                    System.out.println("[Before] Additional advice");
                    return invocation.proceed();
                }
            },
            (MethodMatcher) (method, targetClass) -> method.getName().equals("execute")
        ));

        container.registerBean("service", AopTestServiceImpl.class);

        AopTestService proxy = (AopTestService) container.getBean("service");

        assertDoesNotThrow(() -> proxy.execute("test"));
    }

    @Test
    void shouldNotMatchExcludedMethods() throws Exception {
        DefaultBeanContainer container = new DefaultBeanContainer();

        // 只匹配execute方法
        container.addAdvisor(new DefaultAdvisor(
            new LoggingInterceptor(),
            (MethodMatcher) (method, targetClass) -> method.getName().equals("execute")
        ));

        container.registerBean("service", AopTestServiceImpl.class);

        AopTestService proxy = (AopTestService) container.getBean("service");

        // 捕获System.out输出
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // 调用不匹配的方法
            proxy.anotherMethod();

            String output = outContent.toString();
            assertFalse(output.contains("[日志] 调用方法: anotherMethod"),
                "Should not log excluded methods");
        } finally {
            System.setOut(originalOut);
        }
    }
}
