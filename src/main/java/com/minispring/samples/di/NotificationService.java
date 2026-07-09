package com.minispring.samples.di;

/**
 * 构造器注入示例：ConstructorResolver 会解析此构造器参数
 */
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void notify(String message) {
        System.out.println("   [构造器注入] NotificationService 处理消息");
        repository.save(message);
    }
}
