# 阶段2 - 依赖注入支持实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现Bean之间的依赖关系注入，支持构造器注入和Setter注入，理解依赖解析算法

**架构:** 扩展DefaultBeanContainer，添加ConstructorResolver和DependencyResolver，实现递归依赖解析

**技术栈:** Java 17, JUnit 5, Maven（无外部依赖）

---

## 前置准备

### Task 0: 创建依赖注入相关包结构

**Files:**
- Create: `src/main/java/com/minispring/factory/instantiator/`
- Create: `src/main/java/com/minispring/factory/dependency/`
- Create: `src/test/java/com/minispring/factory/instantiator/`
- Create: `src/test/java/com/minispring/factory/dependency/`

**Step 1: 创建目录结构**

Run: `mkdir -p src/main/java/com/minispring/factory/instantiator src/main/java/com/minispring/factory/dependency src/test/java/com/minispring/factory/instantiator src/test/java/com/minispring/factory/dependency`

**Step 2: 提交目录结构**

```bash
git add .
git commit -m "feat: 创建依赖注入相关包结构"
```

---

## 构造器注入

### Task 1: 设计ConstructorResolver

**Files:**
- Create: `src/main/java/com/minispring/factory/instantiator/ConstructorResolver.java`
- Test: `src/test/java/com/minispring/factory/instantiator/ConstructorResolverTest.java`

**Step 1: 编写测试 - 确定构造器解析需求**

```java
package com.minispring.factory.instantiator;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

public class ConstructorResolverTest {

    @Test
    void shouldResolveNoArgsConstructor() {
        ConstructorResolver resolver = new ConstructorResolver();
        Constructor<?> constructor = resolver.resolve(SimpleBean.class);

        assertNotNull(constructor);
        assertEquals(0, constructor.getParameterCount());
    }

    @Test
    void shouldResolveConstructorWithDependencies() {
        ConstructorResolver resolver = new ConstructorResolver();
        Constructor<?> constructor = resolver.resolve(ServiceWithDependency.class);

        assertNotNull(constructor);
        assertEquals(1, constructor.getParameterCount());
        assertEquals(Repository.class, constructor.getParameterTypes()[0]);
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ConstructorResolverTest`
Expected: COMPILATION ERROR - 类不存在

**Step 3: 创建ConstructorResolver**

```java
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
```

**Step 4: 创建测试用的类**

```java
package com.minispring.factory.instantiator;

// 简单Bean
class SimpleBean {
    public SimpleBean() {}
}

// 仓储接口
interface Repository {}

// 仓储实现
class RepositoryImpl implements Repository {
    public RepositoryImpl() {}
}

// 带依赖的服务
class ServiceWithDependency {
    private final Repository repository;

    public ServiceWithDependency(Repository repository) {
        this.repository = repository;
    }
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=ConstructorResolverTest`
Expected: 测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/instantiator/ src/test/java/com/minispring/factory/instantiator/
git commit -m "feat: 实现ConstructorResolver构造器解析器"
```

---

### Task 2: 实现DependencyResolver依赖解析器

**Files:**
- Create: `src/main/java/com/minispring/factory/dependency/DependencyResolver.java`
- Test: `src/test/java/com/minispring/factory/dependency/DependencyResolverTest.java`

**Step 1: 编写测试 - 定义依赖解析行为**

```java
package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DependencyResolverTest {

    @Test
    void shouldResolveSingleDependency() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("repository", Repository.class);

        DependencyResolver resolver = new DependencyResolver(container);
        Object dependency = resolver.resolve(Repository.class);

        assertNotNull(dependency);
        assertTrue(dependency instanceof Repository);
    }

    @Test
    void shouldResolveMultipleDependencies() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("repository1", Repository1.class);
        container.registerBean("repository2", Repository2.class);

        DependencyResolver resolver = new DependencyResolver(container);

        Object dep1 = resolver.resolve(Repository1.class);
        Object dep2 = resolver.resolve(Repository2.class);

        assertNotNull(dep1);
        assertNotNull(dep2);
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=DependencyResolverTest`
Expected: COMPILATION ERROR

**Step 3: 创建DependencyResolver**

```java
package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.BeanNotFoundException;

/**
 * 依赖解析器
 * 根据类型从容器中解析依赖
 */
public class DependencyResolver {

    private final BeanContainer container;

    public DependencyResolver(BeanContainer container) {
        this.container = container;
    }

    /**
     * 根据类型解析依赖
     *
     * @param type 依赖的类型
     * @return 依赖实例
     * @throws BeanNotFoundException 如果依赖找不到
     */
    public Object resolve(Class<?> type) {
        // 简单实现：遍历所有Bean，按类型匹配
        // 后续会优化为按类型索引
        return resolveByType(type);
    }

    /**
     * 按类型解析Bean
     *
     * @param type 目标类型
     * @return 匹配的Bean实例
     */
    private Object resolveByType(Class<?> type) {
        // 尝试按类型名称查找
        String beanName = generateBeanName(type);
        try {
            return container.getBean(beanName);
        } catch (BeanNotFoundException e) {
            // 如果找不到，尝试查找接口或父类的实现
            return findBeanByType(type);
        }
    }

    /**
     * 根据类型生成Bean名称
     * 将类名首字母小写作为Bean名称
     */
    private String generateBeanName(Class<?> type) {
        String simpleName = type.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * 按类型查找Bean（遍历查找）
     * 这是一个临时实现，后续会优化
     */
    private Object findBeanByType(Class<?> type) {
        // TODO: 后续实现按类型索引
        throw new BeanNotFoundException("No bean found for type: " + type.getName());
    }
}
```

**Step 4: 创建测试用的类**

```java
package com.minispring.factory.dependency;

// 测试用的仓储类
class Repository {
    public Repository() {}
}

class Repository1 {
    public Repository1() {}
}

class Repository2 {
    public Repository2() {}
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=DependencyResolverTest`
Expected: 测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/dependency/ src/test/java/com/minispring/factory/dependency/
git commit -m "feat: 实现DependencyResolver依赖解析器"
```

---

### Task 3: 集成构造器注入到容器

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`
- Modify: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 编写测试 - 验证构造器注入**

```java
@Test
void shouldInjectDependenciesViaConstructor() {
    BeanContainer container = new DefaultBeanContainer();

    // 注册依赖
    container.registerBean("repository", Repository.class);
    // 注册需要依赖的Bean
    container.registerBean("service", ServiceWithDependency.class);

    // 获取Service，其依赖应该被自动注入
    ServiceWithDependency service = (ServiceWithDependency) container.getBean("service");

    assertNotNull(service);
    assertNotNull(service.getRepository());
}

@Test
void shouldThrowExceptionWhenDependencyNotFound() {
    BeanContainer container = new DefaultBeanContainer();

    // 注册需要依赖的Bean，但不注册依赖
    container.registerBean("service", ServiceWithDependency.class);

    assertThrows(RuntimeException.class, () -> {
        container.getBean("service");
    });
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=BeanContainerTest#shouldInjectDependenciesViaConstructor`
Expected: 测试失败（依赖未注入）

**Step 3: 修改DefaultBeanContainer支持构造器注入**

```java
package com.minispring.factory;

import com.minispring.factory.dependency.DependencyResolver;
import com.minispring.factory.instantiator.ConstructorResolver;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class DefaultBeanContainer implements BeanContainer {

    private final Map<String, Object> beans = new HashMap<>();
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
        Object bean = beans.get(name);
        if (bean != null) {
            return bean;
        }

        Class<?> clazz = beanDefinitions.get(name);
        if (clazz == null) {
            throw new BeanNotFoundException(name);
        }

        try {
            // 懒初始化DependencyResolver
            if (dependencyResolver == null) {
                dependencyResolver = new DependencyResolver(this);
            }

            bean = createBeanWithDependencies(clazz);
            beans.put(name, bean);
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    /**
     * 创建Bean实例并注入依赖
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
     */
    private Object[] resolveDependencies(Class<?>[] parameterTypes) {
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = dependencyResolver.resolve(parameterTypes[i]);
        }
        return args;
    }
}
```

**Step 4: 更新测试用的类**

```java
package com.minispring.test;

// 仓储接口
interface Repository {}

// 仓储实现
class RepositoryImpl implements Repository {
    public RepositoryImpl() {}
}

// 带依赖的服务
class ServiceWithDependency {
    private final Repository repository;

    public ServiceWithDependency(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: 所有测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/ src/test/java/com/minispring/factory/
git commit -m "feat: 集成构造器注入到容器"
```

---

## Setter注入

### Task 4: 实现Setter注入支持

**Files:**
- Create: `src/main/java/com/minispring/factory/instantiator/SetterInjector.java`
- Test: `src/test/java/com/minispring/factory/instantiator/SetterInjectorTest.java`
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`

**Step 1: 编写测试 - 定义Setter注入行为**

```java
package com.minispring.factory.instantiator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SetterInjectorTest {

    @Test
    void shouldInjectViaSetter() {
        SetterInjector injector = new SetterInjector();
        ServiceWithSetter service = new ServiceWithSetter();
        Repository repository = new RepositoryImpl();

        injector.inject(service, "setRepository", repository);

        assertNotNull(service.getRepository());
        assertTrue(service.getRepository() instanceof RepositoryImpl);
    }

    @Test
    void shouldHandleMultipleSetters() {
        SetterInjector injector = new SetterInjector();
        MultiSetterService service = new MultiSetterService();
        Repository repo1 = new RepositoryImpl();
        Repository repo2 = new RepositoryImpl();

        injector.inject(service, "setRepo1", repo1);
        injector.inject(service, "setRepo2", repo2);

        assertNotNull(service.getRepo1());
        assertNotNull(service.getRepo2());
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=SetterInjectorTest`
Expected: COMPILATION ERROR

**Step 3: 创建SetterInjector**

```java
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
```

**Step 4: 创建测试用的类**

```java
package com.minispring.factory.instantiator;

// 带Setter的服务
class ServiceWithSetter {
    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }
}

// 多Setter服务
class MultiSetterService {
    private Repository repo1;
    private Repository repo2;

    public void setRepo1(Repository repo1) {
        this.repo1 = repo1;
    }

    public void setRepo2(Repository repo2) {
        this.repo2 = repo2;
    }

    public Repository getRepo1() {
        return repo1;
    }

    public Repository getRepo2() {
        return repo2;
    }
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=SetterInjectorTest`
Expected: 测试通过

**Step 6: 集成到DefaultBeanContainer**

修改DefaultBeanContainer，添加Setter注入支持：

```java
// 添加字段
private final SetterInjector setterInjector = new SetterInjector();

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

    // 执行Setter注入（如果有相应的Bean定义）
    performSetterInjection(bean);

    return bean;
}

/**
 * 执行Setter注入
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
 */
private boolean isSetterMethod(Method method) {
    return method.getName().startsWith("set") &&
           method.getParameterCount() == 1 &&
           method.getReturnType() == void.class;
}
```

**Step 7: 提交**

```bash
git add src/main/java/com/minispring/factory/
git commit -m "feat: 实现Setter注入支持"
```

---

## 循环依赖检测

### Task 5: 实现循环依赖检测

**Files:**
- Create: `src/main/java/com/minispring/factory/dependency/CircularDependencyDetector.java`
- Test: `src/test/java/com/minispring/factory/dependency/CircularDependencyDetectorTest.java`
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`

**Step 1: 编写测试 - 验证循环依赖检测**

```java
package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CircularDependencyDetectorTest {

    @Test
    void shouldDetectCircularDependency() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("serviceA", ServiceA.class);
        container.registerBean("serviceB", ServiceB.class);

        assertThrows(RuntimeException.class, () -> {
            container.getBean("serviceA");
        });
    }

    @Test
    void shouldAllowSelfDependency() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("service", ServiceWithSelfDependency.class);

        // 应该正常工作（使用代理或三级缓存）
        Object service = container.getBean("service");

        assertNotNull(service);
    }
}
```

**Step 2: 创建测试用的类**

```java
package com.minispring.factory.dependency;

// 循环依赖示例
class ServiceA {
    private final ServiceB serviceB;

    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }

    public ServiceB getServiceB() {
        return serviceB;
    }
}

class ServiceB {
    private final ServiceA serviceA;

    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }

    public ServiceA getServiceA() {
        return serviceA;
    }
}

// 自依赖（使用Setter）
class ServiceWithSelfDependency {
    private ServiceWithSelfDependency self;

    public ServiceWithSelfDependency() {}

    public void setSelf(ServiceWithSelfDependency self) {
        this.self = self;
    }

    public ServiceWithSelfDependency getSelf() {
        return self;
    }
}
```

**Step 3: 创建CircularDependencyDetector**

```java
package com.minispring.factory.dependency;

import java.util.HashSet;
import java.util.Set;

/**
 * 循环依赖检测器
 * 检测Bean创建过程中的循环依赖
 */
public class CircularDependencyDetector {

    /**
     * 正在创建中的Bean集合
     */
    private final Set<String> beansInCreation = new HashSet<>();

    /**
     * 开始创建Bean
     *
     * @param beanName Bean名称
     * @throws CircularDependencyException 如果检测到循环依赖
     */
    public void beforeCreation(String beanName) {
        if (beansInCreation.contains(beanName)) {
            throw new CircularDependencyException(
                "Circular dependency detected: " + beanName + " is already being created"
            );
        }
        beansInCreation.add(beanName);
    }

    /**
     * Bean创建完成
     *
     * @param beanName Bean名称
     */
    public void afterCreation(String beanName) {
        beansInCreation.remove(beanName);
    }

    /**
     * 循环依赖异常
     */
    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}
```

**Step 4: 集成到DefaultBeanContainer**

```java
// 添加字段
private final CircularDependencyDetector circularDependencyDetector = new CircularDependencyDetector();

// 修改getBean方法
@Override
public Object getBean(String name) {
    Object bean = beans.get(name);
    if (bean != null) {
        return bean;
    }

    Class<?> clazz = beanDefinitions.get(name);
    if (clazz == null) {
        throw new BeanNotFoundException(name);
    }

    try {
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
    } catch (Exception e) {
        // 清理创建状态
        circularDependencyDetector.afterCreation(name);
        throw new RuntimeException("Failed to create bean: " + name, e);
    }
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=CircularDependencyDetectorTest`
Expected: 循环依赖被检测并抛出异常

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/
git commit -m "feat: 实现循环依赖检测"
```

---

## 更新示例应用

### Task 6: 更新示例应用使用自动注入

**Files:**
- Modify: `src/main/java/com/minispring/samples/Application.java`

**Step 1: 更新Application**

```java
package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.repository.UserRepository;
import com.minispring.samples.service.UserService;

/**
 * Mini-Spring示例应用 - 阶段2
 * 演示自动依赖注入
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段2 - 依赖注入) ===");
        System.out.println();

        // 创建容器
        BeanContainer container = new DefaultBeanContainer();

        // 注册Bean（注意：先注册依赖，后注册使用方）
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        // 获取UserService，依赖会自动注入
        UserService userService = (UserService) container.getBean("userService");

        // 使用Bean
        System.out.println("调用UserService:");
        userService.createUser("李四");

        System.out.println();
        System.out.println("=== 依赖注入自动完成！===");
        System.out.println("=== 应用运行完成 ===");
    }
}
```

**Step 2: 修改UserService支持构造器注入**

```java
package com.minispring.samples.service;

import com.minispring.samples.repository.UserRepository;

/**
 * 用户服务类 - 支持构造器注入
 */
public class UserService {

    private final UserRepository userRepository;

    // 构造器注入
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("UserService创建完成，依赖已注入: " + userRepository.getClass().getSimpleName());
    }

    public void createUser(String username) {
        System.out.println("创建用户: " + username);
        if (userRepository != null) {
            userRepository.save(username);
        }
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
```

**Step 3: 运行示例应用**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.Application"`
Expected: 输出显示依赖自动注入成功

**Step 4: 提交**

```bash
git add src/main/java/com/minispring/samples/
git commit -m "feat: 更新示例应用使用自动依赖注入"
```

---

## 阶段2完成检查

### Task 7: 验证阶段2完成

**Files:**
- Create: `docs/phases/phase02-completion-checklist.md`

**Step 1: 创建完成检查清单**

```markdown
# 阶段2完成检查清单

## 功能验证

- [x] ConstructorResolver构造器解析器
- [x] DependencyResolver依赖解析器
- [x] 构造器注入支持
- [x] Setter注入支持
- [x] 循环依赖检测
- [x] 按类型解析依赖

## 测试覆盖

- [x] ConstructorResolver测试
- [x] DependencyResolver测试
- [x] SetterInjector测试
- [x] CircularDependencyDetector测试
- [x] 集成测试（构造器注入）
- [x] 集成测试（Setter注入）
- [x] 循环依赖测试

## 代码质量

- [x] 所有测试通过
- [x] 代码有完整注释
- [x] 符合Java命名规范
- [x] 异常处理完善

## 学习目标达成

- [x] 理解依赖注入的原理
- [x] 理解构造器解析算法
- [x] 理解依赖递归解析
- [x] 理解循环依赖问题及解决方案
```

**Step 2: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 3: 创建阶段2标签**

```bash
git tag phase-2-completed
```

**Step 4: 提交完成文档**

```bash
git add docs/phases/phase02-completion-checklist.md
git commit -m "docs: 完成阶段2检查清单"
```

---

## 阶段2总结

完成本阶段后，你已经实现：
1. 构造器注入机制
2. Setter注入机制
3. 依赖自动解析
4. 循环依赖检测

**下一步：** 进入阶段3，实现Bean生命周期与作用域

---

## 执行说明

本计划使用TDD方法，每个测试先行编写，然后实现最小代码使测试通过。

**执行此计划时，请：**
1. 按照Task顺序依次执行
2. 每个Step完成后验证结果
3. 遇到问题时先检查测试是否正确
4. 保持小步提交，每个功能点单独commit

**相关技能：**
- @superpowers:test-driven-development - TDD详细指导
- @superpowers:systematic-debugging - 遇到问题时使用
