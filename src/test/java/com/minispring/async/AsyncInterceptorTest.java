package com.minispring.async;

import com.minispring.aop.advice.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsyncInterceptor 单元测试：用捕获型 Executor（收集 Runnable，不内联执行）保证确定性
 */
public class AsyncInterceptorTest {

    /** 测试夹具：方法直接标 @Async，便于单元测试拦截器逻辑 */
    public static class Fixture {
        final List<String> log = new ArrayList<>();

        @Async
        public void doVoid(String tag) {
            log.add("void:" + tag);
        }

        @Async
        public CompletableFuture<String> doFuture(String tag) {
            log.add("future:" + tag);
            return CompletableFuture.completedFuture("R:" + tag);
        }

        @Async
        public CompletableFuture<String> doFutureFail() {
            throw new RuntimeException("boom");
        }

        @Async
        public CompletableFuture<String> doFutureInnerFail() {
            return CompletableFuture.failedFuture(new RuntimeException("inner-boom"));
        }

        @Async
        public String doBad() {
            return "x";
        }

        public String doSync() {
            return "sync";
        }
    }

    /** 构造一个包裹目标方法调用的 MethodInvocation */
    private MethodInvocation invocation(Object target, String name, Class<?>[] params, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(name, params);
        return new MethodInvocation() {
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return args; }
            @Override public Object getTarget() { return target; }
            @Override public Object proceed() throws Throwable {
                try {
                    return method.invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };
    }

    private static final Class<?>[] STR = {String.class};

    @Test
    void voidAsyncShouldSubmitAndReturnNull() throws Throwable {
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        Object result = interceptor.around(invocation(fixture, "doVoid", STR, new Object[]{"A"}));

        assertNull(result, "void 异步方法应返回 null");
        assertTrue(fixture.log.isEmpty(), "方法体不应在调用线程执行");
        assertEquals(1, captured.size(), "应提交 1 个任务到 Executor");
        captured.get(0).run();
        assertEquals(List.of("void:A"), fixture.log, "任务运行后才执行方法体");
    }

    @Test
    void completableFutureAsyncShouldCompleteAfterTaskRuns() throws Throwable {
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        @SuppressWarnings("unchecked")
        CompletableFuture<String> future = (CompletableFuture<String>)
                interceptor.around(invocation(fixture, "doFuture", STR, new Object[]{"B"}));

        assertNotNull(future);
        assertFalse(future.isDone(), "任务未运行时 CF 不应完成");
        assertEquals(1, captured.size());
        captured.get(0).run();
        assertTrue(future.isDone());
        assertEquals("R:B", future.get());
    }

    @Test
    void completableFutureAsyncShouldCompleteExceptionallyOnTaskFailure() throws Throwable {
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        @SuppressWarnings("unchecked")
        CompletableFuture<String> future = (CompletableFuture<String>)
                interceptor.around(invocation(fixture, "doFutureFail", new Class<?>[0], new Object[0]));

        captured.get(0).run();
        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void completableFutureAsyncShouldPropagateInnerFutureFailure() throws Throwable {
        // 目标方法返回一个已失败的 CompletableFuture，验证 whenComplete 的 error 分支
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        @SuppressWarnings("unchecked")
        CompletableFuture<String> future = (CompletableFuture<String>)
                interceptor.around(invocation(fixture, "doFutureInnerFail", new Class<?>[0], new Object[0]));

        captured.get(0).run();
        assertTrue(future.isCompletedExceptionally());
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertEquals("inner-boom", ex.getCause().getMessage());
    }

    @Test
    void unsupportedReturnTypeShouldThrow() throws Exception {
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        assertThrows(IllegalStateException.class,
            () -> interceptor.around(invocation(fixture, "doBad", new Class<?>[0], new Object[0])),
            "@Async 方法返回非 void/CompletableFuture 应抛 IllegalStateException");
    }

    @Test
    void nonAsyncMethodShouldProceedSynchronously() throws Throwable {
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture();

        Object result = interceptor.around(invocation(fixture, "doSync", new Class<?>[0], new Object[0]));

        assertEquals("sync", result, "无 @Async 的方法应直接同步 proceed");
        assertTrue(captured.isEmpty(), "非异步方法不应提交 Executor");
    }

    @Test
    void voidAsyncShouldSwallowTaskException() throws Throwable {
        // void 任务的异常应被 printStackTrace 吞掉，不向外传播
        List<Runnable> captured = new ArrayList<>();
        AsyncInterceptor interceptor = new AsyncInterceptor(captured::add);
        Fixture fixture = new Fixture() {
            @Async
            public void doVoid(String tag) {
                throw new RuntimeException("void-boom");
            }
        };

        Object result = interceptor.around(invocation(fixture, "doVoid", STR, new Object[]{"X"}));
        assertNull(result);
        // 运行捕获的任务不应抛异常（被吞）
        assertDoesNotThrow(() -> captured.get(0).run());
    }
}
