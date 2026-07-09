package com.minispring.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MailService 实现；用静态列表记录副作用便于测试断言
 */
public class MailServiceImpl implements MailService {

    public static final List<String> SENT = new ArrayList<>();

    @Override
    public void send(String to) {
        SENT.add(to);
    }

    @Override
    public CompletableFuture<String> fetch(String query) {
        return CompletableFuture.completedFuture("result:" + query);
    }
}
