package com.minispring.event;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.factory.lifecycle.DisposableBean;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 容器生命周期事件集成测试：就绪广播、关闭广播、关闭与销毁顺序
 */
public class EventContainerLifecycleTest {

    public static class RefreshedListener implements ApplicationListener<ContextRefreshedEvent> {
        boolean fired = false;

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            fired = true;
        }
    }

    // 关闭顺序记录用（静态，@BeforeEach 不可行则每条测试新建容器与独立记录器）
    static class OrderRecorder {
        final List<String> log = new ArrayList<>();
    }

    static class CloseWatcher implements ApplicationListener<ContextClosedEvent> {
        final OrderRecorder recorder;

        CloseWatcher(OrderRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            recorder.log.add("close");
        }
    }

    static class DyingBean implements DisposableBean {
        final OrderRecorder recorder;

        DyingBean(OrderRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void destroy() {
            recorder.log.add("destroy");
        }
    }

    @Test
    void refreshShouldFireContextRefreshedEvent() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.registerBean("refreshedListener", RefreshedListener.class);

        container.refresh();

        RefreshedListener listener = (RefreshedListener) container.getBean("refreshedListener");
        assertTrue(listener.fired, "refresh() 应广播 ContextRefreshedEvent");
    }

    @Test
    void destroyShouldFireContextClosedEventBeforeDestroyingSingletons() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        OrderRecorder recorder = new OrderRecorder();

        // 用实例注册：CloseWatcher 与 DyingBean 共享同一个 recorder
        // （DefaultBeanContainer 支持按名注册实例：见下方说明）
        container.registerBeanInstance("closeWatcher", new CloseWatcher(recorder));
        container.registerBeanInstance("dyingBean", new DyingBean(recorder));

        container.refresh();   // eager 实例化 + 注册 CloseWatcher
        container.destroy();   // 应先广播 close，再 destroy

        assertEquals(List.of("close", "destroy"), recorder.log,
            "ContextClosedEvent 应在单例销毁之前广播");
    }
}
