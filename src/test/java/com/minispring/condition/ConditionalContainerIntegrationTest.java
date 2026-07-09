package com.minispring.condition;

import com.minispring.factory.BeanNotFoundException;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 条件装配与容器集成测试：扫描路径评估条件、程序式注册不评估条件
 */
public class ConditionalContainerIntegrationTest {

    @Test
    void shouldRegisterConditionalBeanWhenPropertyMatches() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.getEnvironment().setProperty("feature.x", "true");
        container.scanComponents("com.minispring.condition.test");

        assertDoesNotThrow(() -> container.getBean("alwaysService"));
        assertDoesNotThrow(() -> container.getBean("featureEnabledService"));
    }

    @Test
    void shouldSkipConditionalBeanWhenPropertyAbsent() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.scanComponents("com.minispring.condition.test");

        assertDoesNotThrow(() -> container.getBean("alwaysService"));
        assertThrows(BeanNotFoundException.class, () -> container.getBean("featureEnabledService"));
    }

    @Test
    void shouldSkipConditionalBeanWhenPropertyMismatch() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.getEnvironment().setProperty("feature.x", "false");
        container.scanComponents("com.minispring.condition.test");

        assertThrows(BeanNotFoundException.class, () -> container.getBean("featureEnabledService"));
        assertDoesNotThrow(() -> container.getBean("alwaysService"));
    }

    @Test
    void registerBeanShouldIgnoreConditions() {
        // 程序式注册即便类上有 @ConditionalOnProperty 也应注册
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.registerBean("manual",
                com.minispring.condition.test.FeatureEnabledService.class);
        assertDoesNotThrow(() -> container.getBean("manual"));
    }
}
