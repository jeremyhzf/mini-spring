package com.minispring.async;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Async 与容器集成测试：addAdvisor + 接口代理 + 异步执行
 */
public class AsyncContainerIntegrationTest {

    @BeforeEach
    void clearSent() {
        MailServiceImpl.SENT.clear();
    }

    private DefaultBeanContainer containerWithAsync(List<Runnable> captured) {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.addAdvisor(new DefaultAdvisor(
                new AsyncInterceptor(captured::add),
                (MethodMatcher) (method, targetClass) -> method.isAnnotationPresent(Async.class)
        ));
        container.registerBean("mailService", MailServiceImpl.class);
        return container;
    }

    @Test
    void voidAsyncShouldRunViaProxyNotInline() {
        List<Runnable> captured = new ArrayList<>();
        MailService mail = (MailService) containerWithAsync(captured).getBean("mailService");

        mail.send("alice");

        assertTrue(MailServiceImpl.SENT.isEmpty(), "调用线程不应执行方法体（已提交到 Executor）");
        assertEquals(1, captured.size(), "代理应提交 1 个任务到 Executor");
        captured.get(0).run();
        assertEquals(List.of("alice"), MailServiceImpl.SENT);
    }

    @Test
    void completableFutureAsyncShouldReturnFutureViaProxy() throws Exception {
        List<Runnable> captured = new ArrayList<>();
        MailService mail = (MailService) containerWithAsync(captured).getBean("mailService");

        CompletableFuture<String> future = mail.fetch("q");

        assertNotNull(future);
        assertFalse(future.isDone());
        captured.get(0).run();
        assertTrue(future.isDone());
        assertEquals("result:q", future.get());
    }

    @Test
    void beanShouldBeProxied() {
        List<Runnable> captured = new ArrayList<>();
        Object bean = containerWithAsync(captured).getBean("mailService");
        // 代理对象不是原始实现类（JDK 代理）
        assertNotSame(MailServiceImpl.class, bean.getClass());
        assertInstanceOf(MailService.class, bean);
    }
}
