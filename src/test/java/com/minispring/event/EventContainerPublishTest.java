package com.minispring.event;

import com.minispring.annotation.Autowired;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 容器事件发布集成测试：自动发现监听器、注入发布器、自定义事件分发
 */
public class EventContainerPublishTest {

    static class OrderCreatedEvent extends ApplicationEvent {
        private final String orderId;

        public OrderCreatedEvent(Object source, String orderId) {
            super(source);
            this.orderId = orderId;
        }

        public String getOrderId() {
            return orderId;
        }
    }

    /** 发布方：依赖注入发布器 */
    public static class OrderService {
        @Autowired
        private ApplicationEventPublisher publisher;

        public void place(String orderId) {
            publisher.publishEvent(new OrderCreatedEvent(this, orderId));
        }

        /** 测试用：暴露注入的发布器以便断言注入目标 */
        ApplicationEventPublisher getPublisher() {
            return publisher;
        }
    }

    /** 监听方 */
    public static class OrderCreatedListener implements ApplicationListener<OrderCreatedEvent> {
        final List<String> received = new ArrayList<>();

        @Override
        public void onApplicationEvent(OrderCreatedEvent event) {
            received.add(event.getOrderId());
        }
    }

    @Test
    void shouldPublishEventToAutoDiscoveredListener() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.registerBean("orderService", OrderService.class);
        container.registerBean("orderCreatedListener", OrderCreatedListener.class);

        // 实例化监听器，触发探测器注册
        OrderCreatedListener listener = (OrderCreatedListener) container.getBean("orderCreatedListener");
        OrderService service = (OrderService) container.getBean("orderService");

        service.place("ORD-001");

        assertEquals(List.of("ORD-001"), listener.received, "自定义事件应分发到自动发现的监听器");
    }

    @Test
    void shouldInjectContainerAsPublisher() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.registerBean("orderService", OrderService.class);

        OrderService service = (OrderService) container.getBean("orderService");

        assertSame(container, service.getPublisher(),
            "@Autowired 注入的 ApplicationEventPublisher 应为容器本身");
    }
}
