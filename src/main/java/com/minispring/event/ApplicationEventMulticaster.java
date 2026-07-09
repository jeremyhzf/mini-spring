package com.minispring.event;

/**
 * 应用事件多播器接口
 * 事件引擎核心：维护监听器集合，并把事件路由分发给匹配的监听器。
 */
public interface ApplicationEventMulticaster {

    /**
     * 注册监听器
     */
    void addApplicationListener(ApplicationListener<?> listener);

    /**
     * 移除监听器
     */
    void removeApplicationListener(ApplicationListener<?> listener);

    /**
     * 把事件分发给所有匹配的监听器
     */
    void multicastEvent(ApplicationEvent event);
}
