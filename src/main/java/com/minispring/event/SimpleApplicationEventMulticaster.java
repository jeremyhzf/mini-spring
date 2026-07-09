package com.minispring.event;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * 默认应用事件多播器
 * 维护监听器集合，按事件运行时类型路由分发。
 * Executor == null 时同步执行（默认）；设置后异步执行。
 * ErrorHandler == null 时异常向外传播（默认）；设置后交其处理并继续。
 */
public class SimpleApplicationEventMulticaster implements ApplicationEventMulticaster {

    private final Set<ApplicationListener<?>> listeners = new CopyOnWriteArraySet<>();
    private final Map<ApplicationListener<?>, Class<?>> eventTypeCache = new ConcurrentHashMap<>();

    private volatile Executor executor;
    private volatile ErrorHandler errorHandler;

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        listeners.remove(listener);
        eventTypeCache.remove(listener);
    }

    @Override
    public void multicastEvent(ApplicationEvent event) {
        for (ApplicationListener<?> listener : listeners) {
            invokeListener(listener, event);
        }
    }

    private void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        // 缓存监听器的事件类型，避免每次分发都做反射泛型解析
        Class<?> eventType = eventTypeCache.computeIfAbsent(listener,
                GenericTypeResolver::resolveListenerEventType);
        // null 表示无法解析（裸类型）→ 接收所有事件
        if (eventType != null && !eventType.isAssignableFrom(event.getClass())) {
            return;
        }
        Runnable task = () -> doInvoke(listener, event);
        if (executor != null) {
            executor.execute(task);
        } else {
            task.run();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doInvoke(ApplicationListener listener, ApplicationEvent event) {
        try {
            listener.onApplicationEvent(event);
        } catch (Throwable t) {
            if (errorHandler != null) {
                errorHandler.handleError(t);
            } else if (t instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw (Error) t;
            }
        }
    }

    /**
     * 设置异步执行器；null 表示同步执行（默认）
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * 设置异常处理器；null 表示异常向外传播（默认）
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}
