package com.minispring.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 监听器泛型事件类型解析测试
 */
public class GenericTypeResolverTest {

    /** 测试用具体事件 */
    static class MyEvent extends ApplicationEvent {
        public MyEvent(Object source) {
            super(source);
        }
    }

    /** 命名子类：显式实现 ApplicationListener<MyEvent> */
    static class NamedListener implements ApplicationListener<MyEvent> {
        @Override
        public void onApplicationEvent(MyEvent event) {
        }
    }

    @Test
    void shouldResolveEventTypeFromNamedSubclass() {
        Class<?> type = GenericTypeResolver.resolveListenerEventType(new NamedListener());
        assertEquals(MyEvent.class, type, "命名子类应解析出 MyEvent");
    }

    @Test
    void shouldResolveEventTypeFromAnonymousClass() {
        ApplicationListener<MyEvent> anonymous = new ApplicationListener<>() {
            @Override
            public void onApplicationEvent(MyEvent event) {
            }
        };
        Class<?> type = GenericTypeResolver.resolveListenerEventType(anonymous);
        assertEquals(MyEvent.class, type, "匿名内部类应解析出 MyEvent");
    }

    @Test
    void shouldReturnNullForRawListener() {
        // 裸 ApplicationListener，无泛型实参
        @SuppressWarnings("rawtypes")
        ApplicationListener raw = new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
            }
        };
        Class<?> type = GenericTypeResolver.resolveListenerEventType(raw);
        assertNull(type, "裸类型监听器无法解析，应返回 null（表示接收所有事件）");
    }
}
