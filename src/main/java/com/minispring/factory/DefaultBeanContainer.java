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
import com.minispring.factory.scope.ScopeAnnotation;
import com.minispring.factory.scope.SingletonAnnotation;
import com.minispring.factory.scope.PrototypeAnnotation;
import com.minispring.aop.proxy.ProxyFactory;
import com.minispring.aop.Advisor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.minispring.scanner.ClassPathBeanScanner;
import com.minispring.annotation.Autowired;
import com.minispring.annotation.Qualifier;
import com.minispring.annotation.Value;
import com.minispring.env.Environment;
import com.minispring.env.StandardEnvironment;

/**
 * 默认的Bean容器实现
 *
 * 使用Map存储Bean实例，通过反射创建对象
 */
public class DefaultBeanContainer implements BeanContainer, com.minispring.event.ApplicationEventPublisher {

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
    // 环境
    private Environment environment;
    // AOP代理工厂
    private final ProxyFactory proxyFactory = new ProxyFactory();
    // 事件多播器
    private final com.minispring.event.ApplicationEventMulticaster multicaster =
            new com.minispring.event.SimpleApplicationEventMulticaster();

    /**
     * 默认构造器：自动注册监听器探测器
     */
    public DefaultBeanContainer() {
        registerBeanPostProcessor(new com.minispring.event.ApplicationListenerDetector(multicaster));
    }

    @Override
    public void registerBean(String name, Class<?> clazz) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Bean name cannot be null or empty");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Bean class cannot be null");
        }
        beanDefinitions.put(name, clazz);

        // 解析作用域注解
        parseScopeAnnotation(name, clazz);
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

        // 执行字段注入
        performFieldInjection(bean);

        // 执行@Value注入
        performValueInjection(bean);

        // 执行后处理器前置处理
        bean = applyPostProcessBeforeInitialization(bean);

        // 执行初始化回调
        applyInitializingBean(bean);

        // 执行后处理器后置处理
        bean = applyPostProcessAfterInitialization(bean);

        // 应用AOP代理（如果有）
        bean = applyAopProxy(bean, clazz);

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
            args[i] = resolveDependency(parameterTypes[i]);
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
                    Object dependency = resolveDependency(paramType);
                    method.invoke(bean, dependency);
                } catch (Exception e) {
                    // Setter注入失败不影响Bean创建
                    // 依赖可能不存在，跳过
                }
            }
        }
    }

    /**
     * 执行字段注入（@Autowired）
     *
     * @param bean 目标Bean
     * @throws Exception 注入失败时抛出异常
     */
    private void performFieldInjection(Object bean) throws Exception {
        Class<?> clazz = bean.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Autowired autowired = field.getAnnotation(Autowired.class);

                try {
                    // 获取限定符
                    String qualifier = null;
                    if (field.isAnnotationPresent(Qualifier.class)) {
                        qualifier = field.getAnnotation(Qualifier.class).value();
                    }

                    // 解析依赖
                    Object dependency;
                    if (qualifier != null) {
                        dependency = getBean(qualifier);
                    } else {
                        dependency = resolveDependency(field.getType());
                    }

                    // 设置字段值
                    field.setAccessible(true);
                    field.set(bean, dependency);
                } catch (BeanNotFoundException e) {
                    if (autowired.required()) {
                        throw e;
                    }
                    // 非必须的依赖，跳过
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

    /**
     * 解析作用域注解
     *
     * @param beanName Bean名称
     * @param clazz Bean类型
     */
    private void parseScopeAnnotation(String beanName, Class<?> clazz) {
        // 检查@ScopeAnnotation注解
        ScopeAnnotation scopeAnnotation = clazz.getAnnotation(ScopeAnnotation.class);
        if (scopeAnnotation != null) {
            setBeanScope(beanName, scopeAnnotation.value());
            return;
        }

        // 检查@SingletonAnnotation注解
        if (clazz.isAnnotationPresent(SingletonAnnotation.class)) {
            setBeanScope(beanName, "singleton");
            return;
        }

        // 检查@PrototypeAnnotation注解
        if (clazz.isAnnotationPresent(PrototypeAnnotation.class)) {
            setBeanScope(beanName, "prototype");
        }
    }

    /**
     * 扫描指定包并注册所有组件
     *
     * @param basePackage 要扫描的包名
     * @return 注册的组件数量
     */
    public int scanComponents(String basePackage) {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner(basePackage);
        Set<Class<?>> components = scanner.scan();

        int count = 0;
        for (Class<?> component : components) {
            String beanName = scanner.generateBeanName(component);
            registerBean(beanName, component);
            count++;
        }

        return count;
    }

    /**
     * 获取Bean定义映射（用于依赖解析）
     *
     * @return Bean定义映射
     */
    public Map<String, Class<?>> getBeanDefinitions() {
        return new HashMap<>(beanDefinitions);
    }

    /**
     * 设置环境
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 获取环境
     */
    public Environment getEnvironment() {
        if (environment == null) {
            environment = new StandardEnvironment();
        }
        return environment;
    }

    /**
     * 执行@Value注入
     */
    private void performValueInjection(Object bean) throws Exception {
        Class<?> clazz = bean.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                Value value = field.getAnnotation(Value.class);
                String resolvedValue = getEnvironment().resolvePlaceholders(value.value());

                field.setAccessible(true);

                // 类型转换
                Object convertedValue = convertValue(resolvedValue, field.getType());
                field.set(bean, convertedValue);
            }
        }
    }

    /**
     * 类型转换
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        throw new IllegalArgumentException("Unsupported type: " + targetType);
    }

    /**
     * 判断类型是否为容器内部可解析的依赖（发布器/容器自身）
     */
    private boolean isInternalResolvableType(Class<?> type) {
        return type == com.minispring.event.ApplicationEventPublisher.class
                || type == BeanContainer.class
                || type == DefaultBeanContainer.class;
    }

    /**
     * 统一依赖解析：内部类型返回容器自身，其余走依赖解析器
     */
    private Object resolveDependency(Class<?> type) {
        if (isInternalResolvableType(type)) {
            return this;
        }
        if (dependencyResolver == null) {
            dependencyResolver = new DependencyResolver(this);
        }
        return dependencyResolver.resolve(type);
    }

    /**
     * 添加全局Advisor
     */
    public void addAdvisor(Advisor advisor) {
        proxyFactory.addAdvisor(advisor);
    }

    /**
     * 发布应用事件（委托给多播器）
     *
     * @param event 要发布的事件
     */
    @Override
    public void publishEvent(com.minispring.event.ApplicationEvent event) {
        multicaster.multicastEvent(event);
    }

    /**
     * 暴露多播器，便于配置 Executor / ErrorHandler
     */
    public com.minispring.event.ApplicationEventMulticaster getApplicationEventMulticaster() {
        return multicaster;
    }

    /**
     * 应用AOP代理
     */
    private Object applyAopProxy(Object bean, Class<?> clazz) throws Exception {
        // 检查是否有Advisor需要应用到这个Bean
        // 简化实现：如果有Advisor就创建代理
        if (proxyFactory.hasAdvisors()) {
            ProxyFactory factory = new ProxyFactory();
            factory.setTarget(bean);
            factory.setInterfaces(clazz.getInterfaces());
            factory.addAdvisors(proxyFactory.getAdvisors());
            return factory.getProxy();
        }
        return bean;
    }
}
