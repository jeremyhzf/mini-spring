package com.minispring.samples.event;

import com.minispring.annotation.Autowired;
import com.minispring.event.ApplicationEventPublisher;
import com.minispring.stereotype.Service;

/**
 * 发布方：依赖注入发布器，注册用户时发布事件
 */
@Service
public class UserService {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void register(String name) {
        System.out.println("[UserService] 注册用户: " + name);
        publisher.publishEvent(new UserCreatedEvent(this, name));
    }
}
