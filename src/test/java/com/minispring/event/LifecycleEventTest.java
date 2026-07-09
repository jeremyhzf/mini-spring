package com.minispring.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 生命周期事件测试
 */
public class LifecycleEventTest {

    @Test
    void contextRefreshedEventShouldRetainSource() {
        Object source = new Object();
        ContextRefreshedEvent event = new ContextRefreshedEvent(source);
        assertSame(source, event.getSource(), "ContextRefreshedEvent 应保留 source");
    }

    @Test
    void contextClosedEventShouldRetainSource() {
        Object source = new Object();
        ContextClosedEvent event = new ContextClosedEvent(source);
        assertSame(source, event.getSource(), "ContextClosedEvent 应保留 source");
    }
}
