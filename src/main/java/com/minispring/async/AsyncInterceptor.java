package com.minispring.async;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 异步拦截器
 * 把 @Async 方法提交到 Executor 异步执行：
 * - void 返回：提交任务后立即返回 null（发后不管）；任务内异常 printStackTrace 不外传
 * - CompletableFuture 返回：提交任务，完成后 complete/completeExceptionally，返回该 future
 * - 其他返回类型：抛 IllegalStateException
 * 无 @Async 的方法：直接同步 proceed（防御性）
 */
public class AsyncInterceptor implements AroundAdvice {

    private final Executor executor;

    /**
     * 默认使用守护线程的缓存线程池（避免 demo/测试 JVM 挂起）
     */
    public AsyncInterceptor() {
        this(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }));
    }

    /**
     * 自定义 Executor（测试可用捕获型/同步 Executor 保证确定性）
     */
    public AsyncInterceptor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (!method.isAnnotationPresent(Async.class)) {
            return invocation.proceed();
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            executor.execute(() -> {
                try {
                    invocation.proceed();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
            return null;
        }

        if (CompletableFuture.class.isAssignableFrom(returnType)) {
            // 目标方法自身返回 CompletableFuture；把它的结果传播到外层 future
            CompletableFuture<Object> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    CompletableFuture<?> result = (CompletableFuture<?>) invocation.proceed();
                    result.whenComplete((value, error) -> {
                        if (error != null) {
                            future.completeExceptionally(error);
                        } else {
                            future.complete(value);
                        }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        throw new IllegalStateException(
                "@Async 方法必须返回 void 或 CompletableFuture: " + method);
    }
}
