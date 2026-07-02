# 阶段1 - 简单Bean容器实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现最基础的Bean容器，支持通过反射实例化对象并存储在Map中，理解依赖注入的起点

**架构:** 使用单一BeanContainer接口和默认实现，通过Map存储Bean实例，使用反射API实例化对象

**技术栈:** Java 17, JUnit 5, Maven（无外部依赖）

---

## 前置准备

### Task 0: 项目初始化

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/minispring/`
- Create: `src/test/java/com/minispring/`
- Create: `src/main/resources/`
- Create: `src/test/resources/`

**Step 1: 创建根pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.minispring</groupId>
    <artifactId>mini-spring</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>mini-spring</name>
    <description>从零开发的轻量级Spring框架教学项目</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.9.3</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: 验证Maven配置**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: 创建目录结构**

Run: `mkdir -p src/main/java/com/minispring src/test/java/com/minispring src/main/resources src/test/resources`

**Step 4: 提交初始化配置**

```bash
git add pom.xml
git commit -m "feat: 初始化Maven项目配置"
```

---

## 核心接口定义

### Task 1: 定义BeanContainer接口

**Files:**
- Create: `src/main/java/com/minispring/factory/BeanContainer.java`
- Test: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 编写测试 - 定义接口的基本行为**

```java
package com.minispring.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeanContainerTest {

    @Test
    void shouldDefineBeanContainerInterface() {
        // 这个测试验证接口可以被正确编译和实例化
        BeanContainer container = new DefaultBeanContainer();

        assertNotNull(container);
        assertTrue(container instanceof BeanContainer);
    }

    @Test
    void shouldRegisterAndGetBean() {
        BeanContainer container = new DefaultBeanContainer();

        // 注册一个简单的Bean
        container.registerBean("testBean", SimpleBean.class);

        // 获取Bean
        Object bean = container.getBean("testBean");

        assertNotNull(bean);
        assertTrue(bean instanceof SimpleBean);
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: COMPILATION ERROR - 类不存在

**Step 3: 创建BeanContainer接口**

```java
package com.minispring.factory;

/**
 * Bean容器接口，定义注册和获取Bean的基本操作
 *
 * @author mini-spring
 * @since 1.0.0
 */
public interface BeanContainer {

    /**
     * 注册Bean定义
     *
     * @param name Bean的名称
     * @param clazz Bean的类型
     */
    void registerBean(String name, Class<?> clazz);

    /**
     * 获取Bean实例
     *
     * @param name Bean的名称
     * @return Bean实例
     * @throws BeanNotFoundException 当Bean不存在时抛出
     */
    Object getBean(String name) throws BeanNotFoundException;
}
```

**Step 4: 创建BeanNotFoundException**

```java
package com.minispring.factory;

/**
 * Bean找不到异常
 */
public class BeanNotFoundException extends RuntimeException {

    private final String beanName;

    public BeanNotFoundException(String beanName) {
        super("Bean not found: " + beanName);
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
```

**Step 5: 创建测试用的SimpleBean**

```java
package com.minispring.test;

/**
 * 用于测试的简单Bean类
 */
public class SimpleBean {

    public SimpleBean() {
        System.out.println("SimpleBean instantiated");
    }

    public void sayHello() {
        System.out.println("Hello from SimpleBean");
    }
}
```

**Step 6: 再次运行测试**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: COMPILATION ERROR - DefaultBeanContainer不存在

**Step 7: 提交接口定义**

```bash
git add src/main/java/com/minispring/factory/
git commit -m "feat: 定义BeanContainer接口和BeanNotFoundException"
```

---

### Task 2: 实现DefaultBeanContainer

**Files:**
- Create: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`

**Step 1: 创建DefaultBeanContainer实现**

```java
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
```

**Step 2: 运行测试**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: 测试通过

**Step 3: 添加SimpleBean到正确的包**

Run: `mv src/test/java/com/minispring/factory/SimpleBean.java src/test/java/com/minispring/test/SimpleBean.java 2>/dev/null || mkdir -p src/test/java/com/minispring/test && cat > src/test/java/com/minispring/test/SimpleBean.java << 'EOF'
package com.minispring.test;

public class SimpleBean {
    public SimpleBean() {
        System.out.println("SimpleBean instantiated");
    }

    public void sayHello() {
        System.out.println("Hello from SimpleBean");
    }
}
EOF`

**Step 4: 更新测试导入**

```java
package com.minispring.factory;

import com.minispring.test.SimpleBean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeanContainerTest {

    @Test
    void shouldDefineBeanContainerInterface() {
        BeanContainer container = new DefaultBeanContainer();

        assertNotNull(container);
        assertTrue(container instanceof BeanContainer);
    }

    @Test
    void shouldRegisterAndGetBean() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("testBean", SimpleBean.class);

        Object bean = container.getBean("testBean");

        assertNotNull(bean);
        assertTrue(bean instanceof SimpleBean);
    }

    @Test
    void shouldReturnSameInstanceForSameBeanName() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("testBean", SimpleBean.class);

        Object bean1 = container.getBean("testBean");
        Object bean2 = container.getBean("testBean");

        assertSame(bean1, bean2, "应该返回同一个实例");
    }
}
```

**Step 5: 运行测试验证单例行为**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: 所有测试通过

**Step 6: 提交实现**

```bash
git add src/main/java/com/minispring/factory/DefaultBeanContainer.java
git add src/test/java/com/minispring/
git commit -m "feat: 实现DefaultBeanContainer基础功能"
```

---

### Task 3: 添加异常情况测试

**Files:**
- Modify: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 添加Bean不存在的测试**

```java
@Test
void shouldThrowExceptionWhenBeanNotFound() {
    BeanContainer container = new DefaultBeanContainer();

    assertThrows(BeanNotFoundException.class, () -> {
        container.getBean("nonExistentBean");
    });
}
```

**Step 2: 运行测试**

Run: `mvn test -Dtest=BeanContainerTest#shouldThrowExceptionWhenBeanNotFound`
Expected: 测试通过

**Step 3: 添加参数验证测试**

```java
@Test
void shouldThrowExceptionWhenNameIsNull() {
    BeanContainer container = new DefaultBeanContainer();

    assertThrows(IllegalArgumentException.class, () -> {
        container.registerBean(null, SimpleBean.class);
    });
}

@Test
void shouldThrowExceptionWhenNameIsEmpty() {
    BeanContainer container = new DefaultBeanContainer();

    assertThrows(IllegalArgumentException.class, () -> {
        container.registerBean("", SimpleBean.class);
    });
}

@Test
void shouldThrowExceptionWhenClassIsNull() {
    BeanContainer container = new DefaultBeanContainer();

    assertThrows(IllegalArgumentException.class, () -> {
        container.registerBean("testBean", null);
    });
}
```

**Step 4: 运行测试**

Run: `mvn test -Dtest=BeanContainerTest`
Expected: 所有测试通过

**Step 5: 提交异常处理**

```bash
git add src/test/java/com/minispring/factory/BeanContainerTest.java
git commit -m "test: 添加异常情况测试用例"
```

---

### Task 4: 测试无参构造器场景

**Files:**
- Create: `src/test/java/com/minispring/test/BeanWithoutNoArgConstructor.java`
- Modify: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 创建没有无参构造器的Bean**

```java
package com.minispring.test;

/**
 * 用于测试没有无参构造器的Bean
 */
public class BeanWithoutNoArgConstructor {

    private final String name;

    public BeanWithoutNoArgConstructor(String name) {
        this.name = name;
    }
}
```

**Step 2: 添加测试用例**

```java
@Test
void shouldThrowExceptionWhenNoNoArgConstructor() {
    BeanContainer container = new DefaultBeanContainer();

    container.registerBean("noArgBean", BeanWithoutNoArgConstructor.class);

    assertThrows(RuntimeException.class, () -> {
        container.getBean("noArgBean");
    });
}
```

**Step 3: 运行测试**

Run: `mvn test -Dtest=BeanContainerTest#shouldThrowExceptionWhenNoNoArgConstructor`
Expected: 测试通过，抛出RuntimeException

**Step 4: 提交测试**

```bash
git add src/test/java/com/minispring/test/BeanWithoutNoArgConstructor.java
git add src/test/java/com/minispring/factory/BeanContainerTest.java
git commit -m "test: 添加无参构造器验证测试"
```

---

### Task 5: 创建完整的示例应用

**Files:**
- Create: `src/main/java/com/minispring/samples/Application.java`
- Create: `src/main/java/com/minispring/samples/service/UserService.java`
- Create: `src/main/java/com/minispring/samples/repository/UserRepository.java`

**Step 1: 创建Repository类**

```java
package com.minispring.samples.repository;

/**
 * 用户仓储接口
 */
public class UserRepository {

    public void save(String username) {
        System.out.println("保存用户: " + username);
    }

    public String find(String username) {
        return "用户: " + username;
    }
}
```

**Step 2: 创建Service类**

```java
package com.minispring.samples.service;

import com.minispring.samples.repository.UserRepository;

/**
 * 用户服务类
 */
public class UserService {

    private UserRepository userRepository;

    // 通过Setter注入（目前阶段2才实现，这里先手动设置）
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String username) {
        System.out.println("创建用户: " + username);
        if (userRepository != null) {
            userRepository.save(username);
        }
    }
}
```

**Step 3: 创建应用入口**

```java
package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.repository.UserRepository;
import com.minispring.samples.service.UserService;

/**
 * Mini-Spring示例应用
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 ===");
        System.out.println();

        // 创建容器
        BeanContainer container = new DefaultBeanContainer();

        // 注册Bean
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        // 获取Bean
        UserRepository userRepository = (UserRepository) container.getBean("userRepository");
        UserService userService = (UserService) container.getBean("userService");

        // 手动注入依赖（阶段2将实现自动注入）
        userService.setUserRepository(userRepository);

        // 使用Bean
        System.out.println("调用UserService:");
        userService.createUser("张三");

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
```

**Step 4: 运行示例应用**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.Application"`
Expected: 输出显示容器成功创建和获取Bean

**Step 5: 提交示例应用**

```bash
git add src/main/java/com/minispring/samples/
git commit -m "feat: 添加阶段1示例应用"
```

---

## 阶段1完成检查

### Task 6: 验证阶段1完成

**Files:**
- Create: `docs/phases/phase01-completion-checklist.md`

**Step 1: 创建完成检查清单**

```markdown
# 阶段1完成检查清单

## 功能验证

- [x] BeanContainer接口定义完成
- [x] DefaultBeanContainer实现完成
- [x] 支持通过名称注册Bean
- [x] 支持通过名称获取Bean
- [x] 使用Map存储Bean实例
- [x] 使用反射创建实例
- [x] BeanNotFoundException异常处理
- [x] 参数验证（null、empty检查）
- [x] 单例模式（同一名称返回同一实例）
- [x] 示例应用运行成功

## 测试覆盖

- [x] 基本注册和获取测试
- [x] 单例行为测试
- [x] Bean不存在异常测试
- [x] 参数验证测试
- [x] 无参构造器验证测试

## 代码质量

- [x] 所有测试通过
- [x] 代码有完整注释
- [x] 符合Java命名规范
- [x] 异常处理完善

## 学习目标达成

- [x] 理解容器的基本概念
- [x] 理解反射API的使用
- [x] 理解Map作为存储结构的选择
- [x] 理解单例模式的应用
```

**Step 2: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 3: 创建阶段1标签**

```bash
git tag phase-1-completed
git push origin phase-1-completed  # 如果有远程仓库
```

**Step 4: 提交完成文档**

```bash
git add docs/phases/phase01-completion-checklist.md
git commit -m "docs: 完成阶段1检查清单"
```

---

## 阶段1总结

完成本阶段后，你已经实现：
1. 最基础的Bean容器
2. 通过反射实例化对象
3. 使用Map存储Bean实例
4. 基本的异常处理
5. 单例模式保证

**下一步：** 进入阶段2，实现依赖注入支持

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
