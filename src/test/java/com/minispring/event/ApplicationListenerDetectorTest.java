package com.minispring.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监听器探测器测试
 */
public class ApplicationListenerDetectorTest {

    static class PingEvent extends ApplicationEvent {
        public PingEvent(Object source) {
            super(source);
        }
    }

    @Test
    void shouldRegisterListenerIntoMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        ApplicationListenerDetector detector = new ApplicationListenerDetector(multicaster);

        boolean[] fired = {false};
        ApplicationListener<PingEvent> listener = new ApplicationListener<>() {
            @Override
            public void onApplicationEvent(PingEvent event) {
                fired[0] = true;
            }
        };

        // 探测器原样返回 bean，并把监听器注册进多播器
        Object returned = detector.postProcessAfterInitialization("listener", listener);
        assertSame(listener, returned, "探测器应原样返回 bean");

        multicaster.multicastEvent(new PingEvent(this));
        assertTrue(fired[0], "探测器应已把监听器注册进多播器");
    }

    @Test
    void shouldIgnoreNonListenerBean() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        ApplicationListenerDetector detector = new ApplicationListenerDetector(multicaster);

        Object plain = new Object();
        Object returned = detector.postProcessAfterInitialization("plain", plain);

        assertSame(plain, returned, "非监听器 bean 应原样返回");
        // 注册一个真实监听器验证多播器确实没有被非监听器污染（仅此一个监听器能收到）
        boolean[] fired = {false};
        ApplicationListener<PingEvent> listener = new ApplicationListener<>() {
            @Override
            public void onApplicationEvent(PingEvent event) {
                fired[0] = true;
            }
        };
        detector.postProcessAfterInitialization("listener", listener);
        multicaster.multicastEvent(new PingEvent(this));
        assertTrue(fired[0]);
    }
}
