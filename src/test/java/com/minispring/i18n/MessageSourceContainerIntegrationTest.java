package com.minispring.i18n;

import com.minispring.annotation.Autowired;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 容器注入 MessageSource 集成测试
 */
public class MessageSourceContainerIntegrationTest {

    /** 容器实例化的 Bean（跨包，须 public） */
    public static class Greeter {
        @Autowired
        private MessageSource messageSource;

        public String hello(String name, Locale locale) {
            return messageSource.getMessage("greeting", new Object[]{name}, locale);
        }
    }

    @Test
    void shouldInjectConfiguredMessageSource() {
        StaticMessageSource ms = new StaticMessageSource();
        ms.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.setMessageSource(ms);
        container.registerBean("greeter", Greeter.class);

        Greeter greeter = (Greeter) container.getBean("greeter");
        assertEquals("你好,Alice", greeter.hello("Alice", Locale.CHINESE),
            "@Autowired 注入的应为容器配置的 MessageSource");
    }

    @Test
    void shouldReturnNullWhenNotConfigured() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        // 未 setMessageSource → getMessageSource 为 null（向后兼容，不自动创建）
        assertNull(container.getMessageSource());
    }
}
