package com.minispring.samples.di;

/**
 * 通知仓储（被注入的依赖）
 */
public class NotificationRepository {

    public void save(String message) {
        System.out.println("   [仓储] 保存通知: " + message);
    }
}
