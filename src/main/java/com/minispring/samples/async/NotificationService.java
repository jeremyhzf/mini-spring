package com.minispring.samples.async;

import com.minispring.async.Async;

import java.util.concurrent.CompletableFuture;

/**
 * 通知服务接口：@Async 标在接口方法上
 */
public interface NotificationService {

    @Async
    void notify(String message);

    @Async
    CompletableFuture<String> prepare(String message);
}
