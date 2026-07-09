package com.minispring.event;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 监听器泛型事件类型解析工具
 * 从 ApplicationListener<E> 中提取 E，决定监听器关心的事件类型。
 * 对应 Spring 的 ResolvableType。
 */
public final class GenericTypeResolver {

    private GenericTypeResolver() {
    }

    /**
     * 解析监听器关心的事件类型
     *
     * @param listener 监听器
     * @return 事件类型；若无法解析（裸类型、类型变量等）返回 null
     */
    public static Class<?> resolveListenerEventType(ApplicationListener<?> listener) {
        if (listener == null) {
            return null;
        }
        return resolveEventType(listener.getClass());
    }

    private static Class<?> resolveEventType(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }

        // 1. 检查直接实现的泛型接口
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            Class<?> resolved = resolveFromType(genericInterface);
            if (resolved != null) {
                return resolved;
            }
        }

        // 2. 检查泛型父类
        Class<?> fromSuper = resolveFromType(clazz.getGenericSuperclass());
        if (fromSuper != null) {
            return fromSuper;
        }

        // 3. 沿接口继承链与父类链向上查找
        for (Class<?> ifc : clazz.getInterfaces()) {
            Class<?> resolved = resolveEventType(ifc);
            if (resolved != null) {
                return resolved;
            }
        }
        return resolveEventType(clazz.getSuperclass());
    }

    private static Class<?> resolveFromType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            if (parameterized.getRawType() == ApplicationListener.class) {
                Type arg = parameterized.getActualTypeArguments()[0];
                if (arg instanceof Class<?> clazz) {
                    return clazz;
                }
                // 类型变量或嵌套参数化类型，无法解析为具体 Class
                return null;
            }
            // 该参数化接口的原始类型可能间接实现 ApplicationListener，递归
            if (parameterized.getRawType() instanceof Class<?> rawClass) {
                return resolveEventType(rawClass);
            }
        } else if (type instanceof Class<?> clazz) {
            return resolveEventType(clazz);
        }
        return null;
    }
}
