package com.minispring.factory.instantiator;

import java.lang.reflect.Constructor;

/**
 * 构造器解析器
 * 负责解析类的构造器，用于依赖注入
 */
public class ConstructorResolver {

    /**
     * 解析类的构造器
     *
     * @param clazz 要解析的类
     * @return 最合适的构造器
     */
    public Constructor<?> resolve(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // 优先使用无参构造器
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }

        // 如果没有无参构造器，返回第一个构造器
        if (constructors.length > 0) {
            return constructors[0];
        }

        throw new IllegalStateException("No constructor found for class: " + clazz.getName());
    }
}
