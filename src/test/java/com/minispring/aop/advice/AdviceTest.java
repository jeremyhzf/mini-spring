package com.minispring.aop.advice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdviceTest {

    @Test
    void shouldDefineBeforeAdvice() {
        BeforeAdvice advice = (method, args, target) -> System.out.println("Before");
        assertNotNull(advice);
    }

    @Test
    void shouldDefineAfterAdvice() {
        AfterAdvice advice = (method, args, target, returnValue, exception) -> {
            if (exception == null) {
                System.out.println("After: " + returnValue);
            } else {
                System.out.println("After: " + exception);
            }
        };
        assertNotNull(advice);
    }

    @Test
    void shouldDefineAroundAdvice() {
        AroundAdvice advice = invocation -> {
            System.out.println("Around before");
            Object result = invocation.proceed();
            System.out.println("Around after");
            return result;
        };
        assertNotNull(advice);
    }
}
