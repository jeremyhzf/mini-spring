package com.minispring.factory;

import com.minispring.factory.dependency.CircularDependencyDetector;
import com.minispring.factory.dependency.DependencyResolver;
import com.minispring.factory.instantiator.ConstructorResolver;
import com.minispring.factory.instantiator.SetterInjector;
import com.minispring.factory.lifecycle.InitializingBean;
import com.minispring.factory.lifecycle.DisposableBean;
import com.minispring.factory.lifecycle.BeanPostProcessor;
import com.minispring.factory.scope.ScopeRegistry;
import com.minispring.factory.scope.Scope;
import com.minispring.factory.scope.SingletonScope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    // Bean后处理器列表
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();
    // 作用域注册表
    private final ScopeRegistry scopeRegistry = new ScopeRegistry();
    // Bean作用域映射
    private final Map<String, String> beanScopes = new HashMap<>();

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

    /**
     * 注册Bean后处理器
     *
     * @param postProcessor 后处理器
     */
    public void registerBeanPostProcessor(BeanPostProcessor postProcessor) {
        if (postProcessor != null) {
            postProcessors.add(postProcessor);
        }
    }

    /**
     * 设置Bean的作用域
     *
     * @param beanName Bean名称
     * @param scopeName 作用域名称
     */
    public void setBeanScope(String beanName, String scopeName) {
        beanScopes.put(beanName, scopeName);
    }

    @Override
    public Object getBean(String name) {
        // 检查是否已有缓存的实例（用于单例）
        Object bean = beans.get(name);
        if (bean != null) {
            return bean;
        }

        Class<?> clazz = beanDefinitions.get(name);
        if (clazz == null) {
            throw new BeanNotFoundException(name);
        }

        // 获取Bean的作用域
        String scopeName = beanScopes.getOrDefault(name, "singleton");
        Scope scope = scopeRegistry.getScope(scopeName);

        try {
            if (dependencyResolver == null) {
                dependencyResolver = new DependencyResolver(this);
            }

            // 对于单例作用域，直接使用原有逻辑
            if ("singleton".equals(scopeName)) {
                circularDependencyDetector.beforeCreation(name);
                bean = createBeanWithDependencies(clazz);
                beans.put(name, bean);
                circularDependencyDetector.afterCreation(name);
                return bean;
            } else {
                // 对于其他作用域，每次创建新实例
                return scope.get(name, this, () -> createBeanWithDependencies(clazz));
            }
        } catch (CircularDependencyDetector.CircularDependencyException e) {
            throw e;
        } catch (BeanNotFoundException e) {
            circularDependencyDetector.afterCreation(name);
            throw e;
        } catch (Exception e) {
            circularDependencyDetector.afterCreation(name);
            throw new RuntimeException("Failed to get bean: " + name, e);
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

        // 执行后处理器前置处理
        bean = applyPostProcessBeforeInitialization(bean);

        // 执行初始化回调
        applyInitializingBean(bean);

        // 执行后处理器后置处理
        bean = applyPostProcessAfterInitialization(bean);

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

    /**
     * 应用后处理器前置处理
     */
    private Object applyPostProcessBeforeInitialization(Object bean) {
        for (BeanPostProcessor processor : postProcessors) {
            bean = processor.postProcessBeforeInitialization(bean.getClass().getSimpleName(), bean);
        }
        return bean;
    }

    /**
     * 应用后处理器后置处理
     */
    private Object applyPostProcessAfterInitialization(Object bean) {
        for (BeanPostProcessor processor : postProcessors) {
            bean = processor.postProcessAfterInitialization(bean.getClass().getSimpleName(), bean);
        }
        return bean;
    }

    /**
     * 应用初始化回调
     */
    private void applyInitializingBean(Object bean) {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    /**
     * 销毁所有单例Bean
     */
    public void destroy() {
        Scope singletonScope = scopeRegistry.getScope("singleton");
        if (singletonScope instanceof SingletonScope) {
            ((SingletonScope) singletonScope).destroy();
        }

        // 调用DisposableBean的destroy方法
        for (Object bean : beans.values()) {
            if (bean instanceof DisposableBean) {
                ((DisposableBean) bean).destroy();
            }
        }
        beans.clear();
    }
}
