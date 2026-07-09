package com.minispring.samples.event;

import com.minispring.event.ApplicationEvent;

/**
 * 用户创建事件
 */
public class UserCreatedEvent extends ApplicationEvent {

    private final String name;

    public UserCreatedEvent(Object source, String name) {
        super(source);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
