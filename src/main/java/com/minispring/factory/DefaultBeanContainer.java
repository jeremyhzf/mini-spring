package com.minispring.factory;

import com.minispring.factory.dependency.DependencyResolver;
import com.minispring.factory.instantiator.ConstructorResolver;

import java.lang.reflect.Constructor;
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

    private final ConstructorResolver constructorResolver = new ConstructorResolver();
    private DependencyResolver dependencyResolver;

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
            // 懒初始化DependencyResolver
            if (dependencyResolver == null) {
                dependencyResolver = new DependencyResolver(this);
            }

            bean = createBeanWithDependencies(clazz);
            beans.put(name, bean);
            return bean;
        } catch (BeanNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    /**
     * 创建Bean实例并注入依赖
     *
     * @param clazz Bean的类型
     * @return 新创建的Bean实例
     */
    private Object createBeanWithDependencies(Class<?> clazz) throws Exception {
        Constructor<?> constructor = constructorResolver.resolve(clazz);
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        if (parameterTypes.length == 0) {
            // 无参构造器
            return constructor.newInstance();
        } else {
            // 有参构造器 - 需要注入依赖
            Object[] args = resolveDependencies(parameterTypes);
            return constructor.newInstance(args);
        }
    }

    /**
     * 解析构造器参数依赖
     *
     * @param parameterTypes 构造器参数类型数组
     * @return 解析后的依赖对象数组
     */
    private Object[] resolveDependencies(Class<?>[] parameterTypes) {
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = dependencyResolver.resolve(parameterTypes[i]);
        }
        return args;
    }
}
