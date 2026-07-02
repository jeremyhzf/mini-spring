package com.minispring.factory;

import com.minispring.annotation.Autowired;
import com.minispring.stereotype.Component;
import com.minispring.stereotype.Repository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnnotationDrivenTest {

    @Test
    void shouldSupportComponentScan() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        // 扫描测试包
        int count = container.scanComponents("com.minispring.scanner.test");

        assertTrue(count > 0);

        // 获取扫描到的Bean
        Object bean = container.getBean("testComponent");
        assertNotNull(bean);
    }

    @Test
    void shouldSupportFieldInjection() {
        DefaultBeanContainer container = new DefaultBeanContainer();

        container.registerBean("repository", TestRepository.class);
        container.registerBean("service", TestService.class);

        TestService service = (TestService) container.getBean("service");

        assertNotNull(service.repository);
    }

    @Component
    static class TestComponent {}

    @Repository
    static class TestRepository {}

    static class TestService {
        @Autowired
        private TestRepository repository;
    }
}
