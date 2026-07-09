package com.minispring.samples.event;

import com.minispring.event.ApplicationListener;
import com.minispring.stereotype.Component;

/**
 * 监听方：收到用户创建事件
 */
@Component
public class UserCreatedListener implements ApplicationListener<UserCreatedEvent> {

    @Override
    public void onApplicationEvent(UserCreatedEvent event) {
        System.out.println("[监听器] 收到用户创建事件: " + event.getName());
    }
}
