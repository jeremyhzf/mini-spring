# 阶段3 - Bean生命周期与作用域实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现Bean的完整生命周期管理，包括初始化回调、销毁回调、后处理器机制，以及不同作用域支持

**架构:** 扩展DefaultBeanContainer，添加生命周期回调接口、BeanPostProcessor机制和作用域管理

**技术栈:** Java 17, JUnit 5, Maven（无外部依赖）

---

## 前置准备

### Task 0: 创建生命周期相关包结构

**Files:**
- Create: `src/main/java/com/minispring/factory/lifecycle/`
- Create: `src/main/java/com/minispring/factory/scope/`
- Create: `src/test/java/com/minispring/factory/lifecycle/`
- Create: `src/test/java/com/minispring/factory/scope/`

**Step 1: 创建目录结构**

Run: `mkdir -p src/main/java/com/minispring/factory/lifecycle src/main/java/com/minispring/factory/scope src/test/java/com/minispring/factory/lifecycle src/test/java/com/minispring/factory/scope`

**Step 2: 提交目录结构**

```bash
git add .
git commit -m "feat: 创建生命周期相关包结构"
```

---

## 生命周期接口

### Task 1: 定义生命周期回调接口

**Files:**
- Create: `src/main/java/com/minispring/factory/lifecycle/InitializingBean.java`
- Create: `src/main/java/com/minispring/factory/lifecycle/DisposableBean.java`
- Create: `src/main/java/com/minispring/factory/lifecycle/BeanPostProcessor.java`
- Test: `src/test/java/com/minispring/factory/lifecycle/LifecycleTest.java`

**Step 1: 编写测试 - 定义生命周期行为**

```java
package com.minispring.factory.lifecycle;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LifecycleTest {

    @Test
    void shouldCallInitializingBean() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("lifecycleBean", LifecycleBean.class);

        LifecycleBean bean = (LifecycleBean) container.getBean("lifecycleBean");

        assertTrue(bean.isInitialized());
        assertEquals("init called", bean.getStatus());
    }

    @Test
    void shouldCallBeanPostProcessorBeforeInitialization() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("processor", TestPostProcessor.class);
        container.registerBean("bean", LifecycleBean.class);

        LifecycleBean bean = (LifecycleBean) container.getBean("bean");

        assertEquals("post-processed", bean.getStatus());
    }
}
```

**Step 2: 创建InitializingBean接口**

```java
package com.minispring.factory.lifecycle;

/**
 * Bean初始化接口
 * Bean实现此接口后，在属性设置完成后会调用afterPropertiesSet方法
 */
public interface InitializingBean {

    /**
     * 在Bean的所有属性设置完成后调用
     * 用于执行初始化逻辑
     */
    void afterPropertiesSet();
}
```

**Step 3: 创建DisposableBean接口**

```java
package com.minispring.factory.lifecycle;

/**
 * Bean销毁接口
 * Bean实现此接口后，在容器关闭时会调用destroy方法
 */
public interface DisposableBean {

    /**
     * 在Bean销毁时调用
     * 用于执行清理逻辑
     */
    void destroy();
}
```

**Step 4: 创建BeanPostProcessor接口**

```java
package com.minispring.factory.lifecycle;

/**
 * Bean后处理器接口
 * 允许在Bean初始化前后对Bean进行自定义处理
 */
public interface BeanPostProcessor {

    /**
     * 在Bean初始化前调用
     *
     * @param beanName Bean名称
     * @param bean Bean实例
     * @return 处理后的Bean实例
     */
    default Object postProcessBeforeInitialization(String beanName, Object bean) {
        return bean;
    }

    /**
     * 在Bean初始化后调用
     *
     * @param beanName Bean名称
     * @param bean Bean实例
     * @return 处理后的Bean实例
     */
    default Object postProcessAfterInitialization(String beanName, Object bean) {
        return bean;
    }
}
```

**Step 5: 创建测试用的类**

```java
package com.minispring.factory.lifecycle;

class LifecycleBean implements InitializingBean, DisposableBean {
    private boolean initialized = false;
    private boolean destroyed = false;
    private String status = "created";

    @Override
    public void afterPropertiesSet() {
        this.initialized = true;
        this.status = "init called";
    }

    @Override
    public void destroy() {
        this.destroyed = true;
        this.status = "destroyed";
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public String getStatus() {
        return status;
    }
}

class TestPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (bean instanceof LifecycleBean) {
            ((LifecycleBean) bean).setStatus("post-processed");
        }
        return bean;
    }
}
```

**Step 6: 运行测试**

Run: `mvn test -Dtest=LifecycleTest`
Expected: 测试通过

**Step 7: 提交**

```bash
git add src/main/java/com/minispring/factory/lifecycle/ src/test/java/com/minispring/factory/lifecycle/
git commit -m "feat: 定义生命周期回调接口"
```

---

## 集成生命周期到容器

### Task 2: 集成生命周期回调到容器

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`
- Modify: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 修改DefaultBeanContainer支持生命周期**

```java
// 添加字段
private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

// 添加注册后处理器的方法
public void registerBeanPostProcessor(BeanPostProcessor postProcessor) {
    if (postProcessor != null) {
        postProcessors.add(postProcessor);
    }
}

// 修改createBeanWithDependencies方法
private Object createBeanWithDependencies(Class<?> clazz) throws Exception {
    Constructor<?> constructor = constructorResolver.resolve(clazz);
    Class<?>[] parameterTypes = constructor.getParameterTypes();

    Object bean;
    if (parameterTypes.length == 0) {
        bean = constructor.newInstance();
    } else {
        Object[] args = resolveDependencies(parameterTypes);
        bean = constructor.newInstance(args);
    }

    // 执行Setter注入
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
```

**Step 2: 运行测试**

Run: `mvn test -Dtest=LifecycleTest`
Expected: 测试通过

**Step 3: 添加销毁方法测试**

```java
@Test
void shouldCallDisposableBean() {
    BeanContainer container = new DefaultBeanContainer();
    container.registerBean("lifecycleBean", LifecycleBean.class);

    LifecycleBean bean = (LifecycleBean) container.getBean("lifecycleBean");

    // 调用容器的销毁方法（需要添加）
    if (container instanceof DefaultBeanContainer) {
        ((DefaultBeanContainer) container).destroy();
    }

    assertTrue(bean.isDestroyed());
}
```

**Step 4: 添加destroy方法到DefaultBeanContainer**

```java
/**
 * 销毁所有单例Bean
 */
public void destroy() {
    for (Object bean : beans.values()) {
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }
    }
}
```

**Step 5: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/ src/test/java/com/minispring/factory/
git commit -m "feat: 集成生命周期回调到容器"
```

---

## 作用域管理

### Task 3: 实现作用域支持

**Files:**
- Create: `src/main/java/com/minispring/factory/scope/Scope.java`
- Create: `src/main/java/com/minispring/factory/scope/SingletonScope.java`
- Create: `src/main/java/com/minispring/factory/scope/PrototypeScope.java`
- Test: `src/test/java/com/minispring/factory/scope/ScopeTest.java`

**Step 1: 编写测试 - 定义作用域行为**

```java
package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {

    @Test
    void shouldReturnSameInstanceForSingleton() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("singleton", SingletonBean.class);

        Object bean1 = container.getBean("singleton");
        Object bean2 = container.getBean("singleton");

        assertSame(bean1, bean2);
    }

    @Test
    void shouldReturnDifferentInstanceForPrototype() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("prototype", PrototypeBean.class);

        Object bean1 = container.getBean("prototype");
        Object bean2 = container.getBean("prototype");

        assertNotSame(bean1, bean2);
    }
}
```

**Step 2: 创建Scope接口**

```java
package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;

/**
 * Bean作用域接口
 * 定义Bean的实例化策略
 */
public interface Scope {

    /**
     * 获取作用域名称
     */
    String getName();

    /**
     * 获取该作用域下的Bean实例
     *
     * @param beanName Bean名称
     * @param beanFactory Bean工厂
     * @param beanCreator Bean创建器
     * @return Bean实例
     */
    Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator);

    /**
     * Bean创建器函数式接口
     */
    @FunctionalInterface
    interface BeanCreator {
        Object create() throws Exception;
    }
}
```

**Step 3: 创建SingletonScope**

```java
package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例作用域
 * 容器中只存在一个实例
 */
public class SingletonScope implements Scope {

    private final ConcurrentHashMap<String, Object> instances = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "singleton";
    }

    @Override
    public Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator) {
        return instances.computeIfAbsent(beanName, name -> {
            try {
                return beanCreator.create();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton bean: " + name, e);
            }
        });
    }

    /**
     * 销毁所有单例Bean
     */
    public void destroy() {
        instances.clear();
    }
}
```

**Step 4: 创建PrototypeScope**

```java
package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;

/**
 * 原型作用域
 * 每次请求都创建新实例
 */
public class PrototypeScope implements Scope {

    @Override
    public String getName() {
        return "prototype";
    }

    @Override
    public Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator) {
        try {
            return beanCreator.create();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create prototype bean: " + name, e);
        }
    }
}
```

**Step 5: 创建测试用的类**

```java
package com.minispring.factory.scope;

class SingletonBean {
    public SingletonBean() {}
}

class PrototypeBean {
    public PrototypeBean() {}
}
```

**Step 6: 运行测试**

Run: `mvn test -Dtest=ScopeTest`
Expected: 测试通过

**Step 7: 提交**

```bash
git add src/main/java/com/minispring/factory/scope/ src/test/java/com/minispring/factory/scope/
git commit -m "feat: 实现作用域支持"
```

---

## 集成作用域到容器

### Task 4: 集成作用域管理到容器

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`
- Create: `src/main/java/com/minispring/factory/scope/ScopeRegistry.java`

**Step 1: 创建ScopeRegistry**

```java
package com.minispring.factory.scope;

import java.util.HashMap;
import java.util.Map;

/**
 * 作用域注册表
 * 管理所有作用域实例
 */
public class ScopeRegistry {

    private final Map<String, Scope> scopes = new HashMap<>();

    public ScopeRegistry() {
        // 注册默认作用域
        registerScope("singleton", new SingletonScope());
        registerScope("prototype", new PrototypeScope());
    }

    /**
     * 注册作用域
     */
    public void registerScope(String name, Scope scope) {
        scopes.put(name, scope);
    }

    /**
     * 获取作用域
     */
    public Scope getScope(String name) {
        Scope scope = scopes.get(name);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown scope: " + name);
        }
        return scope;
    }
}
```

**Step 2: 修改DefaultBeanContainer支持作用域**

```java
// 添加字段
private final ScopeRegistry scopeRegistry = new ScopeRegistry();
private final Map<String, String> beanScopes = new HashMap<>();

// 添加设置作用域的方法
public void setBeanScope(String beanName, String scopeName) {
    beanScopes.put(beanName, scopeName);
}

// 修改getBean方法
@Override
public Object getBean(String name) {
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

        // 使用作用域获取Bean
        return scope.get(name, this, () -> createBeanWithDependencies(clazz));
    } catch (Exception e) {
        throw new RuntimeException("Failed to get bean: " + name, e);
    }
}
```

**Step 3: 从createBeanWithDependencies中移除缓存逻辑**

```java
// 移除之前的单例缓存逻辑，由Scope管理
private Object createBeanWithDependencies(Class<?> clazz) throws Exception {
    // ... 保持原有实现，但不再管理缓存
}
```

**Step 4: 添加destroyScopedBeans方法**

```java
/**
 * 销毁所有作用域Bean
 */
public void destroy() {
    Scope singletonScope = scopeRegistry.getScope("singleton");
    if (singletonScope instanceof SingletonScope) {
        ((SingletonScope) singletonScope).destroy();
    }

    // 调用DisposableBean的destroy方法
    for (Object bean : ((SingletonScope) singletonScope).getInstances()) {
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }
    }
}
```

**Step 5: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/
git commit -m "feat: 集成作用域管理到容器"
```

---

## 注解支持

### Task 5: 实现作用域注解

**Files:**
- Create: `src/main/java/com/minispring/factory/scope/Scope.java` (注解)
- Create: `src/main/java/com/minispring/factory/scope/Singleton.java`
- Create: `src/main/java/com/minispring/factory/scope/Prototype.java`

**Step 1: 创建@Scope注解**

```java
package com.minispring.factory.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用域注解
 * 用于标识Bean的作用域
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {

    /**
     * 作用域名称
     */
    String value() default "singleton";
}
```

**Step 2: 创建@Singleton注解**

```java
package com.minispring.factory.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 单例注解
 * 标识Bean为单例作用域
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Singleton {
}
```

**Step 3: 创建@Prototype注解**

```java
package com.minispring.factory.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 原型注解
 * 标识Bean为原型作用域
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Prototype {
}
```

**Step 4: 修改DefaultBeanContainer支持注解解析**

```java
// 修改registerBean方法，支持注解解析
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
 * 解析作用域注解
 */
private void parseScopeAnnotation(String beanName, Class<?> clazz) {
    // 检查@Scope注解
    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
    if (scopeAnnotation != null) {
        setBeanScope(beanName, scopeAnnotation.value());
        return;
    }

    // 检查@Singleton注解
    if (clazz.isAnnotationPresent(Singleton.class)) {
        setBeanScope(beanName, "singleton");
        return;
    }

    // 检查@Prototype注解
    if (clazz.isAnnotationPresent(Prototype.class)) {
        setBeanScope(beanName, "prototype");
    }
}
```

**Step 5: 运行测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/scope/
git commit -m "feat: 实现作用域注解支持"
```

---

## 更新示例应用

### Task 6: 更新示例应用演示生命周期

**Files:**
- Modify: `src/main/java/com/minispring/samples/Application.java`
- Create: `src/main/java/com/minispring/samples/lifecycle/`

**Step 1: 创建生命周期Bean示例**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.InitializingBean;
import com.minispring.factory.lifecycle.DisposableBean;
import com.minispring.samples.repository.UserRepository;

public class UserService implements InitializingBean, DisposableBean {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("1. UserService构造器调用");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("2. UserService初始化回调执行");
        System.out.println("   依赖检查: userRepository = " + (userRepository != null ? "已注入" : "未注入"));
    }

    public void createUser(String username) {
        System.out.println("   创建用户: " + username);
        userRepository.save(username);
    }

    @Override
    public void destroy() {
        System.out.println("3. UserService销毁回调执行");
    }
}
```

**Step 2: 创建后处理器示例**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.BeanPostProcessor;

public class LoggingPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 前置处理: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 后置处理: " + beanName);
        return bean;
    }
}
```

**Step 3: 更新Application**

```java
package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.lifecycle.LoggingPostProcessor;
import com.minispring.samples.lifecycle.UserService;
import com.minispring.samples.repository.UserRepository;

public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段3 - 生命周期) ===");
        System.out.println();

        BeanContainer container = new DefaultBeanContainer();

        // 注册后处理器
        ((DefaultBeanContainer) container).registerBeanPostProcessor(new LoggingPostProcessor());

        // 注册Bean
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        System.out.println("--- 获取UserService Bean ---");
        UserService userService = (UserService) container.getBean("userService");

        System.out.println();
        System.out.println("--- 使用UserService ---");
        userService.createUser("王五");

        System.out.println();
        System.out.println("--- 销毁容器 ---");
        ((DefaultBeanContainer) container).destroy();

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
```

**Step 4: 运行示例应用**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.Application"`
Expected: 输出显示完整的生命周期流程

**Step 5: 提交**

```bash
git add src/main/java/com/minispring/samples/
git commit -m "feat: 更新示例应用演示生命周期"
```

---

## 阶段3完成检查

### Task 7: 验证阶段3完成

**Files:**
- Create: `docs/phases/phase03-completion-checklist.md`

**Step 1: 创建完成检查清单**

```markdown
# 阶段3完成检查清单

## 功能验证

- [x] InitializingBean初始化回调
- [x] DisposableBean销毁回调
- [x] BeanPostProcessor后处理器
- [x] 单例作用域
- [x] 原型作用域
- [x] 作用域注解支持

## 测试覆盖

- [x] 生命周期接口测试
- [x] 初始化回调测试
- [x] 后处理器测试
- [x] 作用域测试
- [x] 注解支持测试

## 代码质量

- [x] 所有测试通过
- [x] 代码有完整注释
- [x] 符合Java命名规范
- [x] 异常处理完善

## 学习目标达成

- [x] 理解Bean生命周期
- [x] 理解后处理器模式
- [x] 理解作用域概念
- [x] 理解责任链模式在后处理器中的应用
```

**Step 2: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 3: 创建阶段3标签**

```bash
git tag phase-3-completed
```

**Step 4: 提交完成文档**

```bash
git add docs/phases/phase03-completion-checklist.md
git commit -m "docs: 完成阶段3检查清单"
```

---

## 阶段3总结

完成本阶段后，你已经实现：
1. InitializingBean和DisposableBean生命周期回调
2. BeanPostProcessor后处理器机制
3. Singleton和Prototype作用域支持
4. 作用域注解支持

**下一步：** 进入阶段4，实现注解驱动开发
