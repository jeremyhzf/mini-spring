package com.minispring.factory.instantiator;

import java.lang.reflect.Method;

/**
 * Setter注入器
 * 通过反射调用Setter方法注入依赖
 */
public class SetterInjector {

    /**
     * 通过Setter方法注入依赖
     *
     * @param target 目标对象
     * @param setterName Setter方法名
     * @param value 要注入的值
     */
    public void inject(Object target, String setterName, Object value) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        if (setterName == null || setterName.isEmpty()) {
            throw new IllegalArgumentException("Setter name cannot be null or empty");
        }

        try {
            Class<?> clazz = target.getClass();
            Method setter = findSetterMethod(clazz, setterName, value.getClass());

            if (setter == null) {
                throw new IllegalArgumentException("Setter method not found: " + setterName);
            }

            setter.invoke(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject via setter: " + setterName, e);
        }
    }

    /**
     * 查找Setter方法
     */
    private Method findSetterMethod(Class<?> clazz, String setterName, Class<?> paramType) {
        try {
            return clazz.getMethod(setterName, paramType);
        } catch (NoSuchMethodException e) {
            // 尝试查找兼容类型
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(setterName) &&
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(paramType)) {
                    return method;
                }
            }
            return null;
        }
    }
}
