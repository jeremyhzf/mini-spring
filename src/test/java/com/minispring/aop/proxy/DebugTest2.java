package com.minispring.aop.proxy;

import com.minispring.aop.advice.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DebugTest2 {

    @Test
    void testExceptionHandlingWithDebug() {
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
            System.out.println("AfterAdvice executed");
            System.out.println("  exception param: " + exception);
            System.out.println("  exception class: " + (exception != null ? exception.getClass() : "null"));
            if (exception != null) {
                System.out.println("  exception message: " + exception.getMessage());
                executionOrder.add("after-exception:" + exception.getMessage());
            }
        });
        
        TestInterface proxy = (TestInterface) factory.getProxy();
        
        System.out.println("About to call executeWithError()");
        try {
            proxy.executeWithError();
            executionOrder.add("no-exception");
            System.out.println("No exception thrown");
        } catch (RuntimeException e) {
            executionOrder.add("caught-runtime:" + e.getMessage());
            System.out.println("Caught RuntimeException: " + e.getMessage());
        } catch (Exception e) {
            executionOrder.add("caught-exception:" + e.getMessage());
            System.out.println("Caught Exception: " + e.getClass().getName() + " - " + e.getMessage());
        }
        
        System.out.println("Execution order: " + executionOrder);
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
