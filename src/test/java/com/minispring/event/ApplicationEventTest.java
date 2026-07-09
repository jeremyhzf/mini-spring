package com.minispring.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 事件基础类型契约测试
 */
public class ApplicationEventTest {

    @Test
    void shouldRetainEventSource() {
        Object source = new Object();
        ApplicationEvent event = new ApplicationEvent(source) {
        };
        assertSame(source, event.getSource(), "ApplicationEvent 应保留 source");
    }
}
