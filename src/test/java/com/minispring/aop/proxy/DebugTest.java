package com.minispring.aop.proxy;

import com.minispring.aop.advice.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DebugTest {

    @Test
    void testExceptionHandling() {
        List<String> executionOrder = new ArrayList<>();
        
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new TestService());
        factory.setInterfaces(TestInterface.class);
        
        factory.addAdvice((BeforeAdvice) (method, args, target) -> {
            executionOrder.add("before");
            System.out.println("BeforeAdvice executed");
        });
        
        factory.addAdvice((AfterAdvice) (method, args, target, returnValue, exception) -> {
            executionOrder.add("after");
            System.out.println("AfterAdvice executed, exception: " + (exception != null ? exception.getMessage() : "null"));
            if (exception != null) {
                executionOrder.add("after-exception:" + exception.getMessage());
            }
        });
        
        TestInterface proxy = (TestInterface) factory.getProxy();
        
        try {
            proxy.executeWithError();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            System.out.println("Caught exception: " + e.getMessage());
            executionOrder.add("caught:" + e.getMessage());
        }
        
        System.out.println("Execution order: " + executionOrder);
        
        // 验证AfterAdvice被执行
        assertTrue(executionOrder.contains("after"), "AfterAdvice should be executed");
        assertTrue(executionOrder.contains("after-exception:test error"), "AfterAdvice should receive exception");
    }
    
    interface TestInterface {
        void executeWithError();
    }
    
    class TestService implements TestInterface {
        @Override
        public void executeWithError() {
            System.out.println("Target method throwing exception");
            throw new RuntimeException("test error");
        }
    }
}
