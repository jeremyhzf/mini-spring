package com.minispring.aop.proxy;

import com.minispring.aop.advice.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdviceOrderTest {

    @Test
    void shouldExecuteAdvicesInCorrectOrder() {
        // 记录执行顺序
        List<String> executionOrder = new ArrayList<>();
        
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestService());
        factory.setInterfaces(TestInterface.class);
        
        // 添加BeforeAdvice
        factory.addAdvice((BeforeAdvice) (method, args, target) -> {
            executionOrder.add("before1");
        });
        
        // 添加AroundAdvice
        factory.addAdvice((AroundAdvice) invocation -> {
            executionOrder.add("around1-before");
            Object result = invocation.proceed();
            executionOrder.add("around1-after");
            return result;
        });
        
        // 添加另一个AroundAdvice
        factory.addAdvice((AroundAdvice) invocation -> {
            executionOrder.add("around2-before");
            Object result = invocation.proceed();
            executionOrder.add("around2-after");
            return result;
        });
        
        // 添加AfterAdvice
        factory.addAdvice((AfterAdvice) (method, args, target, returnValue, exception) -> {
            executionOrder.add("after1");
        });
        
        TestInterface proxy = (TestInterface) factory.getProxy();
        proxy.execute();
        
        // 验证执行顺序
        List<String> expected = List.of(
            "before1",
            "around1-before", "around2-before", "around2-after", "around1-after",
            "after1"
        );

        assertEquals(expected, executionOrder, "Advice执行顺序不正确");
    }
    
    @Test
    void shouldExecuteAfterAdviceEvenWhenExceptionThrown() {
        List<String> executionOrder = new ArrayList<>();
        
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestService());
        factory.setInterfaces(TestInterface.class);
        
        factory.addAdvice((BeforeAdvice) (method, args, target) -> {
            executionOrder.add("before");
        });
        
        factory.addAdvice((AroundAdvice) invocation -> {
            executionOrder.add("around-before");
            try {
                return invocation.proceed();
            } finally {
                executionOrder.add("around-finally");
            }
        });
        
        factory.addAdvice((AfterAdvice) (method, args, target, returnValue, exception) -> {
            executionOrder.add("after");
            if (exception != null) {
                executionOrder.add("after-exception:" + exception.getMessage());
            }
        });
        
        TestInterface proxy = (TestInterface) factory.getProxy();
        
        assertThrows(RuntimeException.class, () -> proxy.executeWithError());
        
        // 验证AfterAdvice被执行，即使抛出异常
        assertTrue(executionOrder.contains("after"));
        assertTrue(executionOrder.contains("after-exception:test error"));
    }
    
    interface TestInterface {
        void execute();
        void executeWithError();
    }
    
    class TestService implements TestInterface {
        @Override
        public void execute() {
            System.out.println("target");
        }
        
        @Override
        public void executeWithError() {
            throw new RuntimeException("test error");
        }
    }
}
