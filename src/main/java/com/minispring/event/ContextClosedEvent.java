package com.minispring.event;

/**
 * 容器关闭事件
 * 容器开始销毁单例前广播。source 为容器本身。
 */
public class ContextClosedEvent extends ApplicationEvent {

    public ContextClosedEvent(Object source) {
        super(source);
    }
}
