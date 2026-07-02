package com.minispring.factory;

import com.minispring.factory.dependency.CircularDependencyDetector;
import com.minispring.factory.dependency.DependencyResolver;
import com.minispring.factory.instantiator.ConstructorResolver;
import com.minispring.factory.instantiator.SetterInjector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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

    // 构造器解析器
    private final ConstructorResolver constructorResolver = new ConstructorResolver();
    // Setter注入器
    private final SetterInjector setterInjector = new SetterInjector();
    // 依赖解析器
    private DependencyResolver dependencyResolver;
    // 循环依赖检测器
    private final CircularDependencyDetector circularDependencyDetector = new CircularDependencyDetector();

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

            // 检测循环依赖
            circularDependencyDetector.beforeCreation(name);

            bean = createBeanWithDependencies(clazz);
            beans.put(name, bean);

            // 创建完成
            circularDependencyDetector.afterCreation(name);

            return bean;
        } catch (CircularDependencyDetector.CircularDependencyException e) {
            throw e;
        } catch (BeanNotFoundException e) {
            // 清理创建状态
            circularDependencyDetector.afterCreation(name);
            throw e;
        } catch (Exception e) {
            // 清理创建状态
            circularDependencyDetector.afterCreation(name);
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

        Object bean;
        if (parameterTypes.length == 0) {
            // 无参构造器
            bean = constructor.newInstance();
        } else {
            // 有参构造器 - 需要注入依赖
            Object[] args = resolveDependencies(parameterTypes);
            bean = constructor.newInstance(args);
        }

        // 执行Setter注入（如果有相应的Bean定义）
        performSetterInjection(bean);

        return bean;
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

    /**
     * 执行Setter注入
     *
     * @param bean 目标Bean
     */
    private void performSetterInjection(Object bean) {
        // 查找所有Setter方法
        Method[] methods = bean.getClass().getMethods();

        for (Method method : methods) {
            if (isSetterMethod(method)) {
                Class<?> paramType = method.getParameterTypes()[0];
                try {
                    Object dependency = dependencyResolver.resolve(paramType);
                    method.invoke(bean, dependency);
                } catch (Exception e) {
                    // Setter注入失败不影响Bean创建
                    // 依赖可能不存在，跳过
                }
            }
        }
    }

    /**
     * 判断是否是Setter方法
     *
     * @param method 要检查的方法
     * @return 如果是Setter方法返回true
     */
    private boolean isSetterMethod(Method method) {
        return method.getName().startsWith("set") &&
               method.getParameterCount() == 1 &&
               method.getReturnType() == void.class;
    }
}
