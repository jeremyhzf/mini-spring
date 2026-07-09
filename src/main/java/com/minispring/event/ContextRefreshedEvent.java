package com.minispring.event;

/**
 * 容器就绪事件
 * 容器完成单例 eager 实例化后广播。source 为容器本身。
 */
public class ContextRefreshedEvent extends ApplicationEvent {

    public ContextRefreshedEvent(Object source) {
        super(source);
    }
}
