package com.minispring.aop;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

public class AdvisorTest {

    @Test
    void shouldDefineAdvisor() {
        Advisor advisor = new DefaultAdvisor(
            new AroundAdvice() {
                @Override
                public Object around(MethodInvocation invocation) throws Throwable {
                    return invocation.proceed();
                }
            },
            new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    return method.getName().startsWith("save");
                }
            }
        );

        assertNotNull(advisor.getAdvice());
        assertNotNull(advisor.getPointcut());
        assertTrue(advisor.isPerInstance());
    }

    @Test
    void shouldMatchMethodByPointcut() throws NoSuchMethodException {
        Pointcut pointcut = new MethodMatcher() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return method.getName().equals("execute");
            }
        };

        assertTrue(pointcut.getMethodMatcher().matches(
            TestService.class.getMethod("execute"),
            TestService.class
        ));
    }

    @Test
    void shouldNotMatchMethodByPointcut() throws NoSuchMethodException {
        Pointcut pointcut = new MethodMatcher() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return method.getName().equals("execute");
            }
        };

        assertFalse(pointcut.getMethodMatcher().matches(
            TestService.class.getMethod("save"),
            TestService.class
        ));
    }

    @Test
    void classFilterShouldMatchAll() {
        MethodMatcher matcher = new MethodMatcher() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return true;
            }
        };

        assertTrue(matcher.getClassFilter().matches(TestService.class));
        assertTrue(matcher.getClassFilter().matches(String.class));
    }

    static class TestService {
        public void execute() {}
        public void save() {}
    }
}
