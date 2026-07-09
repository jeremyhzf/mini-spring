package com.minispring.event;

/**
 * 应用事件监听器接口
 * 泛型 E 决定该监听器关心的事件类型；多播器据此路由分发。
 */
public interface ApplicationListener<E extends ApplicationEvent> {

    /**
     * 处理事件
     *
     * @param event 发生的事件
     */
    void onApplicationEvent(E event);
}
