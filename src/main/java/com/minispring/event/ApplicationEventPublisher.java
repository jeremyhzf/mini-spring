package com.minispring.event;

/**
 * 事件发布器接口
 * 容器实现本接口，Bean 可通过依赖注入获得发布能力。
 */
public interface ApplicationEventPublisher {

    /**
     * 发布事件
     *
     * @param event 要发布的事件
     */
    void publishEvent(ApplicationEvent event);
}
