package com.minispring.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多播器测试：类型路由、增删、同步执行、异常处理、异步扩展点
 */
public class SimpleApplicationEventMulticasterTest {

    static class EventA extends ApplicationEvent {
        public EventA(Object source) {
            super(source);
        }
    }

    static class EventB extends ApplicationEvent {
        public EventB(Object source) {
            super(source);
        }
    }

    private SimpleApplicationEventMulticaster multicaster;

    @BeforeEach
    void setUp() {
        multicaster = new SimpleApplicationEventMulticaster();
    }

    @Test
    void shouldDispatchOnlyToMatchingType() {
        List<ApplicationEvent> receivedA = new ArrayList<>();
        List<ApplicationEvent> receivedB = new ArrayList<>();

        multicaster.addApplicationListener(new ApplicationListener<EventA>() {
            @Override
            public void onApplicationEvent(EventA event) {
                receivedA.add(event);
            }
        });
        multicaster.addApplicationListener(new ApplicationListener<EventB>() {
            @Override
            public void onApplicationEvent(EventB event) {
                receivedB.add(event);
            }
        });

        multicaster.multicastEvent(new EventA(this));

        assertEquals(1, receivedA.size(), "EventA 监听器应收到 EventA");
        assertTrue(receivedB.isEmpty(), "EventB 监听器不应收到 EventA");
    }

    @Test
    void shouldDispatchToAllEventsListener() {
        boolean[] fired = {false};
        // E = ApplicationEvent，匹配所有事件
        multicaster.addApplicationListener(new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                fired[0] = true;
            }
        });

        multicaster.multicastEvent(new EventB(this));

        assertTrue(fired[0], "ApplicationEvent 监听器应收到任意事件");
    }

    @Test
    void shouldDispatchToRawListener() {
        boolean[] fired = {false};
        @SuppressWarnings("rawtypes")
        ApplicationListener raw = new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                fired[0] = true;
            }
        };
        multicaster.addApplicationListener(raw);

        multicaster.multicastEvent(new EventA(this));

        assertTrue(fired[0], "裸类型监听器（无法解析）应接收所有事件");
    }

    @Test
    void shouldRemoveListener() {
        boolean[] fired = {false};
        ApplicationListener<EventA> listener = new ApplicationListener<>() {
            @Override
            public void onApplicationEvent(EventA event) {
                fired[0] = true;
            }
        };
        multicaster.addApplicationListener(listener);
        multicaster.removeApplicationListener(listener);

        multicaster.multicastEvent(new EventA(this));

        assertFalse(fired[0], "移除后不应再被分发");
    }

    @Test
    void shouldPropagateErrorByDefault() {
        multicaster.addApplicationListener(new ApplicationListener<EventA>() {
            @Override
            public void onApplicationEvent(EventA event) {
                throw new RuntimeException("boom");
            }
        });

        assertThrows(RuntimeException.class, () -> multicaster.multicastEvent(new EventA(this)),
            "默认情况下监听器异常应向外传播");
    }

    @Test
    void shouldContinueAfterErrorWhenHandlerSet() {
        boolean[] failingHandled = {false};
        boolean[] secondFired = {false};

        multicaster.addApplicationListener(new ApplicationListener<EventA>() {
            @Override
            public void onApplicationEvent(EventA event) {
                throw new RuntimeException("boom");
            }
        });
        multicaster.addApplicationListener(new ApplicationListener<EventA>() {
            @Override
            public void onApplicationEvent(EventA event) {
                secondFired[0] = true;
            }
        });
        multicaster.setErrorHandler(t -> failingHandled[0] = true);

        assertDoesNotThrow(() -> multicaster.multicastEvent(new EventA(this)));
        assertTrue(failingHandled[0], "异常应交给 ErrorHandler");
        assertTrue(secondFired[0], "设置 ErrorHandler 后应继续执行后续监听器");
    }

    @Test
    void shouldDelegateToExecutorWhenSet() {
        boolean[] synchronouslyFired = {false};
        List<Runnable> captured = new ArrayList<>();

        multicaster.addApplicationListener(new ApplicationListener<EventA>() {
            @Override
            public void onApplicationEvent(EventA event) {
                synchronouslyFired[0] = true;
            }
        });
        // 捕获型 Executor：只收集 Runnable，不立即执行
        Executor capturingExecutor = captured::add;
        multicaster.setExecutor(capturingExecutor);

        multicaster.multicastEvent(new EventA(this));

        assertFalse(synchronouslyFired[0], "设置 Executor 后不应同步执行监听器");
        assertEquals(1, captured.size(), "应提交 1 个任务到 Executor");

        captured.get(0).run();
        assertTrue(synchronouslyFired[0], "执行捕获的任务后监听器应被触发");
    }
}
