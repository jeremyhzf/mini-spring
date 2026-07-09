package com.minispring.samples.async;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.async.AsyncInterceptor;
import com.minispring.factory.DefaultBeanContainer;

import java.util.concurrent.CompletableFuture;

/**
 * 阶段7-4 - 异步示例
 * 演示：@Async 接口方法经 AOP 代理异步执行（void 发后不管 + CompletableFuture 返回结果）
 */
public class AsyncDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 阶段7-4：异步 @Async 示例 ===\n");
        System.out.println("[main 线程: " + Thread.currentThread().getName() + "]");

        DefaultBeanContainer container = new DefaultBeanContainer();
        // 接入 @Async：AsyncInterceptor + 匹配 @Async 方法的 MethodMatcher
        container.addAdvisor(new DefaultAdvisor(
                new AsyncInterceptor(),
                (MethodMatcher) (method, targetClass) -> method.isAnnotationPresent(com.minispring.async.Async.class)
        ));
        container.registerBean("notificationService", NotificationServiceImpl.class);

        NotificationService service = (NotificationService) container.getBean("notificationService");

        System.out.println("\n--- 调用 notify（void，发后不管）---");
        service.notify("hello");
        System.out.println("[main] notify 已返回（方法体在另一线程执行）");

        System.out.println("\n--- 调用 prepare（CompletableFuture，等待结果）---");
        CompletableFuture<String> future = service.prepare("world");
        System.out.println("[main] prepare 已返回 future，等待结果...");
        String result = future.get();
        System.out.println("[main] prepare 结果: " + result);

        System.out.println("\n=== 阶段7-4 示例结束 ===");
    }
}
