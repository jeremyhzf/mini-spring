package com.minispring.samples.async;

import com.minispring.async.Async;

import java.util.concurrent.CompletableFuture;

/**
 * 通知服务接口：@Async 标在接口方法上
 */
public interface NotificationService {
    /**
     * 异步通知: 无返回值的异步方法
     */
    @Async
    void notify(String message);

    /**
     * 异步准备: 有返回值的异步方法（使用 CompletableFuture）
     */
    @Async
    CompletableFuture<String> prepare(String message);
}
