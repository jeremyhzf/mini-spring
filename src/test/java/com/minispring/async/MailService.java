package com.minispring.async;

import java.util.concurrent.CompletableFuture;

/**
 * 集成测试用接口：@Async 标在接口方法上
 */
public interface MailService {

    @Async
    void send(String to);

    @Async
    CompletableFuture<String> fetch(String query);
}
