package com.minispring.samples.async;

import java.util.concurrent.CompletableFuture;

/**
 * 通知服务实现：方法体打印所在线程，便于观察异步
 */
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notify(String message) {
        System.out.println("[线程 " + Thread.currentThread().getName() + "] 发送通知: " + message);
    }

    @Override
    public CompletableFuture<String> prepare(String message) {
        System.out.println("[线程 " + Thread.currentThread().getName() + "] 准备通知: " + message);
        return CompletableFuture.completedFuture("已准备: " + message);
    }
}
