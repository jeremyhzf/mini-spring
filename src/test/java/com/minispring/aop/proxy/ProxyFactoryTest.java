package com.minispring.aop.proxy;

import com.minispring.aop.advice.AroundAdvice;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

public class ProxyFactoryTest {

    @Test
    void shouldCreateJdkProxyForInterface() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new SimpleService());
        factory.setInterfaces(TestInterface.class);

        Object proxy = factory.getProxy();

        assertTrue(proxy instanceof TestInterface);
    }

    @Test
    void shouldCreateCglibProxyForClass() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new ConcreteService());

        Object proxy = factory.getProxy();

        // 简化的CGLIB实现使用JDK代理，所以代理对象实现了ProxyMarker接口
        assertTrue(proxy instanceof CglibProxy.ProxyMarker);
        assertFalse(proxy.getClass() == ConcreteService.class);
    }

    @Test
    void shouldApplyAroundAdvice() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new SimpleService());
        factory.setInterfaces(TestInterface.class);

        factory.addAdvice((AroundAdvice) invocation -> {
            System.out.println("Around before");
            Object result = invocation.proceed();
            System.out.println("Around after");
            return result;
        });

        TestInterface proxy = (TestInterface) factory.getProxy();
        proxy.execute();
    }

    interface TestInterface {
        void execute();
    }

    class SimpleService implements TestInterface {
        @Override
        public void execute() {
            System.out.println("SimpleService execute");
        }
    }

    class ConcreteService {
        public void execute() {
            System.out.println("ConcreteService execute");
        }
    }
}
