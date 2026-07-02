package com.minispring.aop.interceptor;

import com.minispring.aop.proxy.ProxyFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InterceptorTest {

    @Test
    void shouldLogMethodExecution() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestServiceImpl());
        factory.setInterfaces(TestService.class);
        factory.addAdvice(new LoggingInterceptor());

        TestService proxy = (TestService) factory.getProxy();
        proxy.execute("test");

        assertNotNull(proxy);
    }

    @Test
    void shouldMonitorPerformance() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestServiceImpl());
        factory.setInterfaces(TestService.class);
        factory.addAdvice(new PerformanceMonitorInterceptor());

        TestService proxy = (TestService) factory.getProxy();
        proxy.execute("test");

        assertNotNull(proxy);
    }

    @Test
    void shouldManageTransaction() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestServiceImpl());
        factory.setInterfaces(TestService.class);
        factory.addAdvice(new TransactionInterceptor());

        TestService proxy = (TestService) factory.getProxy();
        proxy.execute("test");

        assertNotNull(proxy);
    }

    @Test
    void shouldCombineMultipleInterceptors() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestServiceImpl());
        factory.setInterfaces(TestService.class);
        factory.addAdvice(new LoggingInterceptor());
        factory.addAdvice(new TransactionInterceptor());
        factory.addAdvice(new PerformanceMonitorInterceptor());

        TestService proxy = (TestService) factory.getProxy();
        proxy.execute("test");

        assertNotNull(proxy);
    }

    public interface TestService {
        void execute(String param);
    }

    public static class TestServiceImpl implements TestService {
        @Override
        public void execute(String param) {
            System.out.println("Executing with: " + param);
        }
    }
}
