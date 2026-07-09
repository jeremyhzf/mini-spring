# 示例集中化与各模块示例补齐 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分散的示例集中到 `com.minispring.samples` 包树下并按阶段分包，为 6 个阶段各补一个独立可运行的 `*Demo` 入口。

**Architecture:** 单 Maven 模块，不引入多模块结构。示例代码全部归入 `com.minispring.samples.<阶段>` 子包，与框架核心代码分离。每个 Demo 是含 `main` 方法的可运行类，消费框架现有 API；新增文件不修改框架核心代码（仅修改示例自己的 UserService）。

**Tech Stack:** Java 17, Maven, JUnit 5（既有测试）, exec-maven-plugin 3.1.0（新增，用于运行 Demo）

## Global Constraints

- JDK 17，`maven.compiler.source/target` 均为 17。
- 单 Maven 模块 `com.minispring:mini-spring:1.0.0-SNAPSHOT`，不拆分多模块。
- 示例仅做 `main` 演示：**不为本批 Demo 新增专门单元测试**（spec 决策）。每个 Demo 的验证手段 = 编译通过 + `mvn exec:java` 运行观察输出 + `mvn test` 全量回归通过。
- 包名一律 `com.minispring.samples.<阶段>`；入口类一律 `XxxDemo`。
- 不得修改框架核心代码（`factory`/`aop`/`web`/`scanner`/`env`/`annotation`/`stereotype` 等模块），唯一例外是示例自己的 `samples/annotation/UserService.java`（新增 `@Value` 字段）。
- 提交信息用中文，遵循项目现有风格（`feat:` / `refactor:` / `build:` / `docs:`）；每个 Task 结尾独立 commit。
- 当前分支为 `feat/samples-reorganization`，所有提交在此分支。

---

## File Structure

**新建（Create）:**
- `src/main/java/com/minispring/samples/ioc/GreetingBean.java` — 阶段1 演示用简单 Bean
- `src/main/java/com/minispring/samples/ioc/BeanContainerDemo.java` — 阶段1 入口
- `src/main/java/com/minispring/samples/di/NotificationRepository.java` — 被依赖 Bean
- `src/main/java/com/minispring/samples/di/NotificationService.java` — 构造器注入示例
- `src/main/java/com/minispring/samples/di/ReportService.java` — Setter 注入示例
- `src/main/java/com/minispring/samples/di/DependencyInjectionDemo.java` — 阶段2 入口（含循环依赖嵌套类）
- `src/main/java/com/minispring/samples/lifecycle/LifecycleBean.java` — InitializingBean + DisposableBean
- `src/main/java/com/minispring/samples/lifecycle/LifecyclePostProcessor.java` — BeanPostProcessor
- `src/main/java/com/minispring/samples/lifecycle/PrototypeBean.java` — @ScopeAnnotation("prototype")
- `src/main/java/com/minispring/samples/lifecycle/LifecycleDemo.java` — 阶段3 入口
- `src/main/java/com/minispring/samples/annotation/AnnotationDemo.java` — 阶段4 入口
- `src/main/java/com/minispring/samples/aop/AopDemo.java` — 阶段5 入口（迁移自 Application）
- `src/main/java/com/minispring/samples/mvc/MvcDemo.java` — 阶段6 入口（迁移自 WebApplication，增强）
- `src/main/java/com/minispring/samples/mvc/UserController.java` — 迁移自 web/samples/

**修改（Modify）:**
- `src/main/java/com/minispring/samples/annotation/UserService.java` — 新增 `@Value` 字段
- `src/test/java/com/minispring/web/samples/WebSampleTest.java` — 更新 UserController 的 import
- `pom.xml` — 新增 exec-maven-plugin
- `README.md` — 更新运行命令与项目结构

**删除（Delete）:**
- `src/main/java/com/minispring/samples/Application.java`
- `src/main/java/com/minispring/web/samples/WebApplication.java`
- `src/main/java/com/minispring/web/samples/UserController.java`（迁移后）
- 空目录 `samples/repository`、`samples/service`

---

## Task 0: pom 声明 exec-maven-plugin

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: `mvn exec:java -Dexec.mainClass=...` 可稳定运行（后续所有 Task 依赖此能力）。

- [ ] **Step 1: 在 pom.xml 的 `<build><plugins>` 中追加 exec-maven-plugin**

将 `pom.xml` 的 `<plugins>` 段（当前只有 maven-compiler-plugin）改为：

```xml
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
```

- [ ] **Step 2: 验证插件可解析、项目仍可编译**

Run: `mvn -q compile`
Expected: BUILD SUCCESS（无报错）

- [ ] **Step 3: 提交**

```bash
git add pom.xml
git commit -m "build: pom 添加 exec-maven-plugin 以支持运行示例"
```

---

## Task 1: 阶段1 Bean 容器示例

**Files:**
- Create: `src/main/java/com/minispring/samples/ioc/GreetingBean.java`
- Create: `src/main/java/com/minispring/samples/ioc/BeanContainerDemo.java`

**Interfaces:**
- Consumes: `com.minispring.factory.DefaultBeanContainer`（`registerBean(String, Class<?>)`, `getBean(String)`）、`com.minispring.factory.BeanNotFoundException`

- [ ] **Step 1: 创建 GreetingBean**

```java
package com.minispring.samples.ioc;

/**
 * 阶段1 演示用简单 Bean
 */
public class GreetingBean {

    public void greet(String name) {
        System.out.println("你好，" + name + "！我是来自 Mini-Spring 容器的 Bean。");
    }
}
```

- [ ] **Step 2: 创建 BeanContainerDemo**

```java
package com.minispring.samples.ioc;

import com.minispring.factory.BeanNotFoundException;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段1 - Bean 容器示例
 * 演示：注册/获取 Bean、单例、BeanNotFoundException
 */
public class BeanContainerDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段1：Bean 容器示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 1. 注册 Bean
        container.registerBean("greeting", GreetingBean.class);

        // 2. 获取 Bean 并使用
        GreetingBean greeting = (GreetingBean) container.getBean("greeting");
        greeting.greet("世界");

        // 3. 单例验证：同名多次获取返回同一实例
        GreetingBean again = (GreetingBean) container.getBean("greeting");
        System.out.println("\n单例验证: " + (greeting == again
                ? "两次获取为同一实例 ✓"
                : "不同实例 ✗"));

        // 4. 异常演示：获取不存在的 Bean
        System.out.println("\n异常演示: 获取不存在的 Bean...");
        try {
            container.getBean("notExist");
        } catch (BeanNotFoundException e) {
            System.out.println("捕获 BeanNotFoundException: " + e.getMessage());
        }

        System.out.println("\n=== 阶段1 示例结束 ===");
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.ioc.BeanContainerDemo"`
Expected: 控制台打印包含 `你好，世界！`、`两次获取为同一实例 ✓`、`捕获 BeanNotFoundException` 三处关键行。

- [ ] **Step 5: 回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/minispring/samples/ioc/
git commit -m "feat: 添加阶段1 Bean容器示例"
```

---

## Task 2: 阶段2 依赖注入示例

**Files:**
- Create: `src/main/java/com/minispring/samples/di/NotificationRepository.java`
- Create: `src/main/java/com/minispring/samples/di/NotificationService.java`
- Create: `src/main/java/com/minispring/samples/di/ReportService.java`
- Create: `src/main/java/com/minispring/samples/di/DependencyInjectionDemo.java`

**Interfaces:**
- Consumes: `DefaultBeanContainer`（构造器注入经 `ConstructorResolver`、Setter 注入经 `SetterInjector`、循环依赖经 `CircularDependencyDetector`）、`com.minispring.factory.dependency.CircularDependencyDetector.CircularDependencyException`

- [ ] **Step 1: 创建 NotificationRepository（被依赖 Bean）**

```java
package com.minispring.samples.di;

/**
 * 通知仓储（被注入的依赖）
 */
public class NotificationRepository {

    public void save(String message) {
        System.out.println("   [仓储] 保存通知: " + message);
    }
}
```

- [ ] **Step 2: 创建 NotificationService（构造器注入示例）**

```java
package com.minispring.samples.di;

/**
 * 构造器注入示例：ConstructorResolver 会解析此构造器参数
 */
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public void notify(String message) {
        System.out.println("   [构造器注入] NotificationService 处理消息");
        repository.save(message);
    }
}
```

- [ ] **Step 3: 创建 ReportService（Setter 注入示例）**

```java
package com.minispring.samples.di;

/**
 * Setter 注入示例：SetterInjector 通过 setXxx + 类型解析注入
 */
public class ReportService {

    private NotificationRepository repository;

    public void setNotificationRepository(NotificationRepository repository) {
        this.repository = repository;
    }

    public void sendReport(String report) {
        System.out.println("   [Setter注入] ReportService 发送报告");
        repository.save(report);
    }
}
```

- [ ] **Step 4: 创建 DependencyInjectionDemo**

```java
package com.minispring.samples.di;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.factory.dependency.CircularDependencyDetector;

/**
 * 阶段2 - 依赖注入示例
 * 演示：构造器注入、Setter 注入、循环依赖检测
 */
public class DependencyInjectionDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段2：依赖注入示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 注册名采用类名首字母小写，便于按类型解析
        container.registerBean("notificationRepository", NotificationRepository.class);
        container.registerBean("notificationService", NotificationService.class);
        container.registerBean("reportService", ReportService.class);

        // 构造器注入
        System.out.println("--- 构造器注入 ---");
        NotificationService notificationService =
                (NotificationService) container.getBean("notificationService");
        notificationService.notify("订单已发货");

        // Setter 注入
        System.out.println("\n--- Setter 注入 ---");
        ReportService reportService = (ReportService) container.getBean("reportService");
        reportService.sendReport("月度销售报告");

        // 循环依赖检测
        System.out.println("\n--- 循环依赖检测 ---");
        container.registerBean("cycleA", CycleA.class);
        container.registerBean("cycleB", CycleB.class);
        try {
            container.getBean("cycleA");
        } catch (CircularDependencyDetector.CircularDependencyException e) {
            System.out.println("检测到循环依赖: " + e.getMessage());
        }

        System.out.println("\n=== 阶段2 示例结束 ===");
    }

    /** 循环依赖演示用嵌套类 */
    static class CycleA {
        public CycleA(CycleB b) {
        }
    }

    /** 循环依赖演示用嵌套类 */
    static class CycleB {
        public CycleB(CycleA a) {
        }
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.di.DependencyInjectionDemo"`
Expected: 控制台打印包含 `[构造器注入]`、`[Setter注入]`、`[仓储] 保存通知: 月度销售报告`、`检测到循环依赖`。

- [ ] **Step 7: 回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/minispring/samples/di/
git commit -m "feat: 添加阶段2 依赖注入示例"
```

---

## Task 3: 阶段3 生命周期与作用域示例

**Files:**
- Create: `src/main/java/com/minispring/samples/lifecycle/LifecycleBean.java`
- Create: `src/main/java/com/minispring/samples/lifecycle/LifecyclePostProcessor.java`
- Create: `src/main/java/com/minispring/samples/lifecycle/PrototypeBean.java`
- Create: `src/main/java/com/minispring/samples/lifecycle/LifecycleDemo.java`

**Interfaces:**
- Consumes: `InitializingBean.afterPropertiesSet()`、`DisposableBean.destroy()`、`BeanPostProcessor.postProcessBeforeInitialization(String,Object)` / `postProcessAfterInitialization(String,Object)`、`DefaultBeanContainer.registerBeanPostProcessor(BeanPostProcessor)` / `destroy()`、`com.minispring.factory.scope.ScopeAnnotation`

- [ ] **Step 1: 创建 LifecycleBean**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.DisposableBean;
import com.minispring.factory.lifecycle.InitializingBean;

/**
 * 演示初始化/销毁生命周期回调的 Bean
 */
public class LifecycleBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println("   [初始化] afterPropertiesSet() 被调用，属性注入完成");
    }

    @Override
    public void destroy() {
        System.out.println("   [销毁] destroy() 被调用，释放资源");
    }
}
```

- [ ] **Step 2: 创建 LifecyclePostProcessor**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.BeanPostProcessor;

/**
 * 演示 BeanPostProcessor 前后置介入
 */
public class LifecyclePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 初始化前: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 初始化后: " + beanName);
        return bean;
    }
}
```

- [ ] **Step 3: 创建 PrototypeBean**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.scope.ScopeAnnotation;

/**
 * 标注为 prototype 作用域：每次获取都是新实例
 */
@ScopeAnnotation("prototype")
public class PrototypeBean {
}
```

- [ ] **Step 4: 创建 LifecycleDemo**

```java
package com.minispring.samples.lifecycle;

import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段3 - Bean 生命周期与作用域示例
 * 演示：初始化/销毁回调、BeanPostProcessor、prototype 作用域
 */
public class LifecycleDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段3：Bean 生命周期与作用域示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 注册后处理器
        container.registerBeanPostProcessor(new LifecyclePostProcessor());

        // 注册并获取 LifecycleBean，观察初始化回调与后处理器介入
        System.out.println("--- 生命周期回调 + 后处理器 ---");
        container.registerBean("lifecycleBean", LifecycleBean.class);
        container.getBean("lifecycleBean");

        // prototype 作用域：每次获取都是新实例
        System.out.println("\n--- prototype 作用域 ---");
        container.registerBean("prototypeBean", PrototypeBean.class);
        PrototypeBean p1 = (PrototypeBean) container.getBean("prototypeBean");
        PrototypeBean p2 = (PrototypeBean) container.getBean("prototypeBean");
        System.out.println("两次获取 prototype Bean 为同一实例? "
                + (p1 == p2 ? "是（错误）" : "否（每次新建 ✓）"));

        // 销毁回调
        System.out.println("\n--- 销毁回调 ---");
        container.destroy();

        System.out.println("\n=== 阶段3 示例结束 ===");
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.lifecycle.LifecycleDemo"`
Expected: 控制台打印包含 `[后处理器] 初始化前`、`[初始化] afterPropertiesSet()`、`否（每次新建 ✓）`、`[销毁] destroy()`。

- [ ] **Step 7: 回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/minispring/samples/lifecycle/
git commit -m "feat: 添加阶段3 生命周期与作用域示例"
```

---

## Task 4: 阶段4 注解驱动示例

**Files:**
- Modify: `src/main/java/com/minispring/samples/annotation/UserService.java`
- Create: `src/main/java/com/minispring/samples/annotation/AnnotationDemo.java`

**Interfaces:**
- Consumes: `DefaultBeanContainer.scanComponents(String)` / `setEnvironment(Environment)`、`StandardEnvironment.setProperty(String,String)`、`@Value`、`@Autowired`、`@Service`/`@Repository`

- [ ] **Step 1: 修改 UserService，新增 @Value 字段**

将 `src/main/java/com/minispring/samples/annotation/UserService.java` 整体替换为：

```java
package com.minispring.samples.annotation;

import com.minispring.annotation.Autowired;
import com.minispring.annotation.Value;
import com.minispring.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Value("${app.name:mini-spring}")
    private String appName;

    public void createUser(String username) {
        System.out.println("   [应用: " + appName + "] 创建用户: " + username);
        userRepository.save(username);
    }
}
```

- [ ] **Step 2: 创建 AnnotationDemo**

```java
package com.minispring.samples.annotation;

import com.minispring.env.StandardEnvironment;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段4 - 注解驱动开发示例
 * 演示：组件扫描、@Autowired、@Value
 */
public class AnnotationDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段4：注解驱动开发示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 配置环境，供 @Value 解析占位符
        StandardEnvironment environment = new StandardEnvironment();
        environment.setProperty("app.name", "Mini-Spring 演示应用");
        container.setEnvironment(environment);

        // 组件扫描：自动注册带 @Service / @Repository 的类
        int count = container.scanComponents("com.minispring.samples.annotation");
        System.out.println("组件扫描: 发现并注册 " + count + " 个组件");

        // 获取 UserService，观察 @Autowired（注入 UserRepository）与 @Value（注入 appName）
        System.out.println("\n--- 调用 UserService（自动注入已生效）---");
        UserService userService = (UserService) container.getBean("userService");
        userService.createUser("张三");

        System.out.println("\n=== 阶段4 示例结束 ===");
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.annotation.AnnotationDemo"`
Expected: 控制台打印包含 `发现并注册 2 个组件`（UserService + UserRepository）、`[应用: Mini-Spring 演示应用] 创建用户: 张三`、`保存用户: 张三`（UserRepository 现有输出）。

- [ ] **Step 5: 回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/minispring/samples/annotation/
git commit -m "feat: 添加阶段4 注解驱动示例"
```

---

## Task 5: 阶段5 AOP 示例迁移

**Files:**
- Create: `src/main/java/com/minispring/samples/aop/AopDemo.java`
- Delete: `src/main/java/com/minispring/samples/Application.java`

**Interfaces:**
- Consumes: `DefaultBeanContainer.addAdvisor(Advisor)` / `scanComponents(String)`、`DefaultAdvisor(Advice, MethodMatcher)`、`TransactionInterceptor`、`LoggingInterceptor`、`IOrderService`

- [ ] **Step 1: 创建 AopDemo（内容迁移自 Application，改包名/类名）**

```java
package com.minispring.samples.aop;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.aop.interceptor.LoggingInterceptor;
import com.minispring.aop.interceptor.TransactionInterceptor;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段5 - AOP 面向切面编程示例
 * 演示：Advisor + 方法切点匹配、代理 Bean、拦截器执行顺序
 */
public class AopDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段5：AOP 面向切面编程示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 事务拦截器：只应用于 create 开头的方法
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(),
                new MethodMatcher() {
                    @Override
                    public boolean matches(java.lang.reflect.Method method, Class<?> targetClass) {
                        return method.getName().startsWith("create");
                    }
                }
        ));

        // 日志拦截器：应用于所有方法
        container.addAdvisor(new DefaultAdvisor(
                new LoggingInterceptor(),
                (MethodMatcher) (method, targetClass) -> true
        ));

        // 组件扫描
        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.aop");
        System.out.println("扫描到 " + count + " 个组件");

        // 获取 OrderService（AOP 代理）
        System.out.println("\n--- 创建订单（触发事务 + 日志）---");
        IOrderService orderService = (IOrderService) container.getBean("orderService");
        orderService.createOrder("ORD-001");

        System.out.println("\n--- 取消订单（仅触发日志）---");
        orderService.cancelOrder("ORD-001");

        System.out.println("\n=== 阶段5 示例结束 ===");
    }
}
```

- [ ] **Step 2: 删除旧入口 Application.java**

Run: `git rm src/main/java/com/minispring/samples/Application.java`
Expected: 文件已删除（`samples/aop/OrderService.java`、`IOrderService.java` 保留不动）

- [ ] **Step 3: 编译验证**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS（确认无残留对旧 Application 的引用）

- [ ] **Step 4: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.aop.AopDemo"`
Expected: 控制台打印包含 `扫描到`、`创建订单: ORD-001`、`取消订单: ORD-001`，且 createOrder 前后出现事务/日志拦截器输出。

- [ ] **Step 5: 回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/minispring/samples/aop/ src/main/java/com/minispring/samples/Application.java
git commit -m "refactor: 迁移阶段5 AOP示例到 samples.aop.AopDemo"
```

---

## Task 6: 阶段6 MVC 示例迁移

**Files:**
- Create: `src/main/java/com/minispring/samples/mvc/MvcDemo.java`
- Create: `src/main/java/com/minispring/samples/mvc/UserController.java`
- Delete: `src/main/java/com/minispring/web/samples/WebApplication.java`
- Delete: `src/main/java/com/minispring/web/samples/UserController.java`
- Modify: `src/test/java/com/minispring/web/samples/WebSampleTest.java`

**Interfaces:**
- Consumes: `DispatcherServlet.registerController(Object)` / `doGet(req,res)` / `doPost(req,res)`、`MockHttpServletRequest(String method, String path)` / `setParameter(String,String)`、`MockHttpServletResponse.getContent()`

- [ ] **Step 1: 创建迁移后的 UserController（改包名为 samples.mvc）**

```java
package com.minispring.samples.mvc;

import com.minispring.web.ModelAndView;
import com.minispring.web.annotation.GetMapping;
import com.minispring.web.annotation.PostMapping;
import com.minispring.web.annotation.RequestMapping;
import com.minispring.web.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器示例
 */
@RequestMapping("/user")
public class UserController {

    private final Map<String, String> users = new HashMap<>();

    /**
     * 列出所有用户
     */
    @GetMapping("/list")
    public ModelAndView listUsers() {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user/list");
        mav.addObject("users", users.values());
        return mav;
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail")
    public String getUserDetail(@RequestParam("id") String id) {
        return "User detail: " + users.get(id);
    }

    /**
     * 创建新用户
     */
    @PostMapping("/create")
    public String createUser(@RequestParam("name") String name) {
        String id = String.valueOf(users.size() + 1);
        users.put(id, name);
        return "User created: " + name + " (ID: " + id + ")";
    }
}
```

- [ ] **Step 2: 创建 MvcDemo（迁移自 WebApplication 并增强：用 MockRequest 实际发请求）**

```java
package com.minispring.samples.mvc;

import com.minispring.web.servlet.DispatcherServlet;

/**
 * 阶段6 - MVC 前端控制器示例
 * 演示：DispatcherServlet 注册控制器、请求映射、参数解析、返回值处理
 */
public class MvcDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 阶段6：MVC 前端控制器示例 ===\n");

        DispatcherServlet servlet = new DispatcherServlet();
        servlet.registerController(new UserController());

        // 先创建一个用户
        System.out.println("--- POST /user/create ---");
        dispatch(servlet, "POST", "/user/create", "name", "张三");

        // 列出用户
        System.out.println("\n--- GET /user/list ---");
        dispatch(servlet, "GET", "/user/list", null, null);

        // 查询用户详情
        System.out.println("\n--- GET /user/detail ---");
        dispatch(servlet, "GET", "/user/detail", "id", "1");

        System.out.println("\n=== 阶段6 示例结束 ===");
    }

    /**
     * 构造 Mock 请求并交由 DispatcherServlet 处理，打印响应内容
     */
    private static void dispatch(DispatcherServlet servlet, String method, String path,
                                 String paramKey, String paramValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (paramKey != null) {
            request.setParameter(paramKey, paramValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        if ("GET".equals(method)) {
            servlet.doGet(request, response);
        } else {
            servlet.doPost(request, response);
        }

        System.out.println("响应: " + response.getContent());
    }
}
```

- [ ] **Step 3: 更新 WebSampleTest 的 import**

原 `WebSampleTest.java` 与 `UserController` 同处 `com.minispring.web.samples` 包，故未显式 import。迁移后 `UserController` 跨包，必须新增 import。将文件顶部：

```java
package com.minispring.web.samples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
```

整体替换为：

```java
package com.minispring.web.samples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
```

- [ ] **Step 4: 删除旧 web/samples 下的源文件**

Run: `git rm src/main/java/com/minispring/web/samples/WebApplication.java src/main/java/com/minispring/web/samples/UserController.java`
Expected: 两文件已删除

- [ ] **Step 5: 编译验证（含测试编译）**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS（验证 WebSampleTest 的新 import 生效、UserController 跨包引用正确）

- [ ] **Step 6: 针对性验证迁移未破坏 Web 测试**

Run: `mvn -q test -Dtest=WebSampleTest`
Expected: WebSampleTest 的 3 个测试全部通过

- [ ] **Step 7: 运行 Demo 验证输出**

Run: `mvn -q exec:java -Dexec.mainClass="com.minispring.samples.mvc.MvcDemo"`
Expected: 控制台打印包含 `响应: User created: 张三 (ID: 1)`、`响应: ` 含 `user/list`、`响应: User detail: 张三`。

- [ ] **Step 8: 全量回归测试**

Run: `mvn -q test`
Expected: 所有测试通过

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/minispring/samples/mvc/ src/test/java/com/minispring/web/samples/WebSampleTest.java src/main/java/com/minispring/web/samples/
git commit -m "refactor: 迁移阶段6 MVC示例到 samples.mvc.MvcDemo"
```

---

## Task 7: README 更新、目录清理与最终验证

**Files:**
- Modify: `README.md`
- Delete: 空目录 `src/main/java/com/minispring/samples/repository`、`src/main/java/com/minispring/samples/service`

- [ ] **Step 1: 更新 README 的"运行示例应用"一节**

将 README 中 `### 运行示例应用` 整节（从该标题到下一个 `### ` 标题之前）替换为：

````markdown
### 运行示例应用

每阶段一个独立可运行的 Demo 入口，集中位于 `com.minispring.samples` 包下：

```bash
# 阶段1 Bean 容器
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.ioc.BeanContainerDemo"

# 阶段2 依赖注入
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.di.DependencyInjectionDemo"

# 阶段3 生命周期与作用域
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.lifecycle.LifecycleDemo"

# 阶段4 注解驱动
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.annotation.AnnotationDemo"

# 阶段5 AOP
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.aop.AopDemo"

# 阶段6 MVC
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.mvc.MvcDemo"
```
````

- [ ] **Step 2: 更新 README 项目结构中的 samples 部分**

将 README"项目结构"代码块中关于 samples 的这几行：

```
│   └── samples/                  # 示例应用
│       ├── Application.java              # AOP 示例入口
│       └── aop/                          # OrderService / IOrderService
│       └── web/samples/                  # WebApplication / UserController
```

替换为：

```
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
│       ├── ioc/                          # 阶段1 BeanContainerDemo
│       ├── di/                           # 阶段2 DependencyInjectionDemo
│       ├── lifecycle/                    # 阶段3 LifecycleDemo
│       ├── annotation/                   # 阶段4 AnnotationDemo（UserService / UserRepository）
│       ├── aop/                          # 阶段5 AopDemo（OrderService / IOrderService）
│       └── mvc/                          # 阶段6 MvcDemo（UserController）
```

- [ ] **Step 3: 删除遗留空目录**

Run: `rmdir src/main/java/com/minispring/samples/repository src/main/java/com/minispring/samples/service 2>/dev/null; ls src/main/java/com/minispring/samples/`
Expected: 列出的子目录为 `annotation aop di ioc lifecycle mvc`（不再有 repository/service）

- [ ] **Step 4: 最终全量构建与测试**

Run: `mvn -q clean test`
Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 5: 提交**

```bash
git add README.md
git commit -m "docs: 更新 README 示例运行命令与项目结构"
```

- [ ] **Step 6: 标记阶段完成（可选）**

如需保留里程碑，可：

```bash
git tag samples-reorganized
```

---

## 完成标准

- 6 个 `*Demo` 入口均存在、可编译、可经 `mvn exec:java` 运行并打印预期输出。
- `samples/annotation/UserService.java` 含 `@Value` 字段。
- 旧入口 `samples/Application.java` 与 `web/samples/` 已删除。
- `WebSampleTest.java` import 已更新，`mvn test` 全量通过。
- `pom.xml` 含 `exec-maven-plugin:3.1.0`。
- `README.md` 运行命令与结构已同步。
- 空目录 `samples/repository`、`samples/service` 已清理。
