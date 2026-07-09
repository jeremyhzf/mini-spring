# 示例集中化与各模块示例补齐 — 设计文档

## 背景

Mini-Spring 是一个分 6 阶段、TDD 驱动的手写 Spring 教学框架（单 Maven 模块）。当前示例存在两个问题：

1. **示例分散**：可运行入口只有 2 个，分处不同包 —— `samples/Application.java`（阶段5 AOP）和 `web/samples/WebApplication.java`（阶段6 MVC）。
2. **多数模块缺示例**：阶段1（Bean容器）、阶段2（DI）、阶段3（生命周期）、阶段4（注解）都没有可运行的示例入口。`samples/annotation/`（UserService + UserRepository）甚至是孤立代码，无入口引用。`samples/lifecycle`、`samples/repository`、`samples/service` 是空目录。

## 目标

- 将所有示例集中到统一的 `com.minispring.samples` 包树下，与框架核心代码分离。
- 为 6 个阶段各补一个独立可运行的 `*Demo` 入口（含 `main`），清晰对应阶段。
- 处理迁移产生的连带影响（测试引用、README、空目录），保证全量 `mvn test` 通过。

## 设计决策（已与用户确认）

| 决策点 | 选择 |
|--------|------|
| 组织方式 | **单模块内集中分包**（不引入多模块 Maven 结构，与现状一致） |
| 示例粒度 | **每阶段一个独立 Demo 入口**（6 个，各自可独立运行） |
| 测试策略 | **仅 `main` 演示**，不为本批示例新增专门测试（与现有 Application/WebApplication 风格一致） |
| MvcDemo | **增强**：用 `MockHttpServletRequest/Response` 实际发请求并打印响应（原 WebApplication 仅打印说明） |
| `@Value` | **演示**：在 AnnotationDemo 中配合 `Environment` 演示 `@Value` 占位符注入 |
| 命名 | 入口统一 `XxxDemo` |

## 目标包结构

```
src/main/java/com/minispring/samples/
├── ioc/            # 阶段1 Bean容器
│   ├── BeanContainerDemo.java
│   └── GreetingBean.java
├── di/             # 阶段2 依赖注入
│   ├── DependencyInjectionDemo.java
│   ├── NotificationRepository.java   # 被依赖的 Bean
│   ├── NotificationService.java      # 构造器注入示例
│   └── ReportService.java            # Setter 注入示例
├── lifecycle/      # 阶段3 生命周期与作用域
│   ├── LifecycleDemo.java
│   ├── LifecycleBean.java            # InitializingBean + DisposableBean
│   ├── LifecyclePostProcessor.java   # BeanPostProcessor
│   └── PrototypeBean.java            # @ScopeAnnotation("prototype")
├── annotation/     # 阶段4 注解驱动
│   ├── AnnotationDemo.java           # 新增入口
│   ├── UserService.java              # 已有，增加 @Value 字段
│   └── UserRepository.java           # 已有
├── aop/            # 阶段5 AOP
│   ├── AopDemo.java                  # 由 samples/Application.java 迁移
│   ├── OrderService.java             # 已有
│   └── IOrderService.java            # 已有
└── mvc/            # 阶段6 MVC
    ├── MvcDemo.java                  # 由 web/samples/WebApplication.java 迁移 + 增强
    └── UserController.java           # 由 web/samples/ 迁移
```

## 各 Demo 详细设计

> 以下演示要点均已对照真实 API 验证（`DefaultBeanContainer`、`DependencyResolver`、`InitializingBean`/`DisposableBean`/`BeanPostProcessor`、`@ScopeAnnotation`、`scanComponents`、`DispatcherServlet` 等）。

### 阶段1 — BeanContainerDemo（`samples/ioc/`）

- **GreetingBean**：简单 POJO，带 `greet()` 打印问候。
- **演示**：`registerBean("greeting", GreetingBean.class)` → `getBean("greeting")` 调用 `greet()`；同一名称二次获取返回同一实例（单例）；演示 `getBean("不存在")` 抛出 `BeanNotFoundException`。
- **关键 API**：`BeanContainer.registerBean` / `getBean`。

### 阶段2 — DependencyInjectionDemo（`samples/di/`）

- **NotificationRepository**：被依赖的 Bean，`save(String)`。
- **NotificationService**：构造器接收 `NotificationRepository`，演示**构造器注入**（`ConstructorResolver` 解析）。
- **ReportService**：含 `setNotificationRepository(...)`，演示 **Setter 注入**（`SetterInjector` 按 `setXxx` + 类型解析）。
- **演示**：注册 3 个 Bean，获取 `notificationService` 与 `reportService`，观察依赖被自动注入；附循环依赖检测演示（注册两个互相构造器依赖的临时类，捕获 `CircularDependencyDetector.CircularDependencyException`）。
- **依赖解析依据**：`DependencyResolver.resolveByType` 先按"类名首字母小写"查 Bean 名，失败则遍历 `beanDefinitions` 按 `isAssignableFrom` 匹配 —— 注册名与类名小写一致最稳妥。

### 阶段3 — LifecycleDemo（`samples/lifecycle/`）

- **LifecycleBean**：`implements InitializingBean, DisposableBean`，在 `afterPropertiesSet()` / `destroy()` 打印阶段标识。
- **LifecyclePostProcessor**：`implements BeanPostProcessor`，在前后置方法打印日志。
- **PrototypeBean**：标注 `@ScopeAnnotation("prototype")`。
- **演示**：`registerBeanPostProcessor(new LifecyclePostProcessor())`；注册并获取 `lifecycleBean`，观察初始化回调与后处理器介入；注册 `prototypeBean`，连续 `getBean` 返回不同实例；`container.destroy()` 观察 `DisposableBean.destroy()` 被调用。
- **作用域依据**：`DefaultBeanContainer.parseScopeAnnotation` 读取 `@ScopeAnnotation`/`@SingletonAnnotation`/`@PrototypeAnnotation`。

### 阶段4 — AnnotationDemo（`samples/annotation/`）

- **修改 UserService**：新增 `@Value("${app.name:mini-spring}") String appName` 字段，在 `createUser` 中打印，展示 `@Value` 注入。
- **演示**：`new StandardEnvironment()` → `setProperty("app.name", "Mini-Spring 演示")` → `container.setEnvironment(env)`；`scanComponents("com.minispring.samples.annotation")`；`getBean("userService")` 调用 `createUser`，观察 `@Autowired`（注入 UserRepository）与 `@Value`（注入 appName）均生效。
- **关键 API**：`scanComponents`、`@Service`/`@Repository`/`@Autowired`/`@Value`、`Environment`。

### 阶段5 — AopDemo（`samples/aop/`）

- **来源**：迁移现有 `samples/Application.java`（逻辑不变，仅调整包名与类名）。
- **演示**：`addAdvisor` 织入 `TransactionInterceptor`（匹配 `create` 开头方法）与 `LoggingInterceptor`（全匹配）；`scanComponents("com.minispring.samples.aop")`；获取 `orderService` 代理，对比 `createOrder`（触发事务+日志）与 `cancelOrder`（仅日志）。
- **关键 API**：`addAdvisor`、`DefaultAdvisor`、`MethodMatcher`、`scanComponents`。

### 阶段6 — MvcDemo（`samples/mvc/`）

- **来源**：迁移 `web/samples/WebApplication.java` + `UserController.java`，并**增强**。
- **增强点**：原 WebApplication 仅注册控制器并打印说明；MvcDemo 改为用 `MockHttpServletRequest` / `MockHttpServletResponse` 实际发起 `GET /user/list`、`GET /user/detail?id=1`、`POST /user/create?name=张三` 请求，调用 `servlet.doGet/doPost`，并打印响应内容。
- **依据**：参考现有 `WebSampleTest` 的 MockRequest 用法。
- **关键 API**：`DispatcherServlet`、`registerController`、`MockHttpServletRequest/Response`。

## 连带变更（必须）

1. **测试引用更新**：`src/test/java/com/minispring/web/samples/WebSampleTest.java` 中 `import com.minispring.web.samples.UserController` → `com.minispring.samples.mvc.UserController`（UserController 迁移后）。
2. **README 更新**：`README.md`"运行示例应用"一节的入口类名由 `com.minispring.samples.Application` / `com.minispring.web.samples.WebApplication` 更新为 6 个新 Demo 类，并补齐其余阶段的运行命令。
3. **目录清理**：删除空目录 `samples/repository`、`samples/service`；`web/samples/` 整体迁移后删除；删除原 `samples/Application.java`。
4. **pom 声明 exec 插件**：`pom.xml` 增加 `org.codehaus.mojo:exec-maven-plugin:3.1.0`，使 6 个 Demo 可经 `mvn exec:java` 稳定运行。

## 运行方式

每个 Demo 独立运行：

```bash
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.ioc.BeanContainerDemo"
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.di.DependencyInjectionDemo"
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.lifecycle.LifecycleDemo"
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.annotation.AnnotationDemo"
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.aop.AopDemo"
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.mvc.MvcDemo"
```

> 配套改动：本次在 `pom.xml` 的 `<build><plugins>` 中声明 `org.codehaus.mojo:exec-maven-plugin:3.1.0`，保证 `mvn exec:java` 在各 Maven 版本下可稳定运行各 Demo。

## 验收标准

- [ ] 6 个 Demo 均存在于对应子包，各自含可编译的 `main` 方法。
- [ ] `samples/annotation/UserService.java` 增加 `@Value` 字段。
- [ ] `samples/Application.java` 与 `web/samples/` 已删除，旧入口不再存在。
- [ ] `WebSampleTest.java` import 已更新，`mvn test` 全量通过（含既有所有测试）。
- [ ] `README.md` 示例运行命令已更新且指向真实存在的类。
- [ ] 空目录 `samples/repository`、`samples/service` 已清理。
- [ ] `pom.xml` 已声明 `exec-maven-plugin`，6 个 Demo 均可经 `mvn exec:java` 运行。

## 不在本次范围（YAGNI）

- 不引入多模块 Maven 结构。
- 不为本批 Demo 新增专门单元测试（保持仅 `main` 演示）。
- 不实现阶段7（事务/异步/事件/国际化等）。
- 不改动框架核心代码（`factory`/`aop`/`web` 等模块本身），仅消费其现有 API。
