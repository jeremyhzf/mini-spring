package com.minispring.annotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AutowiredTest {

    // 测试Autowired注解
    @Test
    void shouldDefineAutowiredAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[0].getAnnotation(Autowired.class));
    }

    @Test
    void shouldDefineQualifierAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[0].getAnnotation(Qualifier.class));
    }

    @Test
    void shouldDefineValueAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[1].getAnnotation(Value.class));
    }

    static class TestService {
        @Autowired
        @Qualifier("repository")
        private Repository repository;

        @Value("${app.name}")
        private String appName;
    }

    interface Repository {}
}
