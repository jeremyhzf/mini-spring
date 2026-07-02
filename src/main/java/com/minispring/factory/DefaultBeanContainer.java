package com.minispring.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认的Bean容器实现
 *
 * 使用Map存储Bean实例，通过反射创建对象
 */
public class DefaultBeanContainer implements BeanContainer {

    /**
     * 存储Bean实例的Map
     * Key: Bean名称
     * Value: Bean实例
     */
    private final Map<String, Object> beans = new HashMap<>();

    /**
     * 存储Bean定义的Map
     * Key: Bean名称
     * Value: Bean类型
     */
    private final Map<String, Class<?>> beanDefinitions = new HashMap<>();

    @Override
    public void registerBean(String name, Class<?> clazz) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Bean name cannot be null or empty");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Bean class cannot be null");
        }
        beanDefinitions.put(name, clazz);
    }

    @Override
    public Object getBean(String name) {
        // 首先检查缓存中是否已有实例
        Object bean = beans.get(name);
        if (bean != null) {
            return bean;
        }

        // 查找Bean定义
        Class<?> clazz = beanDefinitions.get(name);
        if (clazz == null) {
            throw new BeanNotFoundException(name);
        }

        // 使用反射创建实例
        try {
            bean = createBean(clazz);
            beans.put(name, bean);
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    /**
     * 通过反射创建Bean实例
     *
     * @param clazz Bean的类型
     * @return 新创建的Bean实例
     */
    private Object createBean(Class<?> clazz) throws Exception {
        // 使用无参构造器创建实例
        return clazz.getDeclaredConstructor().newInstance();
    }
}
