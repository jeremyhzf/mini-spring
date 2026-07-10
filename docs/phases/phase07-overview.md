# 阶段 7：高级特性 总览

> Mini-Spring 阶段 7 在阶段 1-6（IoC / DI / 生命周期 / 注解 / AOP / MVC）之上，渐进式重建 Spring 的五项企业级高级特性。
> 创建日期：2026-07-10。阶段 7 全部完成。

## 1. 概述

阶段 7 不追求功能完备，而是把每项高级特性拆成**可独立运行、带完整单元测试的最小实现**，让人真正理解 Spring 在背后做了什么。五项特性各走一个独立的 **brainstorming（spec）→ writing-plans（plan）→ subagent-driven / TDD 实现 + review** 闭环。

- **核心容器始终保持零第三方依赖**：仅用 Java 反射与标准库（`java.util.concurrent`、`ResourceBundle`、`ThreadLocal` 等）；Web 层用 provided Servlet API；测试用 JUnit 5。
- **向后兼容**：每项特性都不破坏既有行为；`DefaultBeanContainer` 贯穿所有阶段、是被最多测试间接依赖的类。
- **测试规模**：阶段 7 新增 84 个测试，总数 99 → **183 全绿**。

## 2. 五大特性总览

| 阶段 | 特性 | 新增包 | 关键类型 | spec / plan |
|------|------|--------|----------|-------------|
| 7-1 | 事件机制 | `event/` | `ApplicationEvent`/`ApplicationListener`/`ApplicationEventPublisher`、`ApplicationEventMulticaster`/`SimpleApplicationEventMulticaster`、`GenericTypeResolver`、`ApplicationListenerDetector`、`ContextRefreshedEvent`/`ContextClosedEvent` | [spec](../superpowers/specs/2026-07-09-events-design.md) / [plan](../superpowers/plans/2026-07-09-events.md) |
| 7-2 | 条件装配 | `condition/` | `@Conditional`/`Condition`/`ConditionContext`、`ConditionEvaluator`、`@ConditionalOnProperty`/`OnPropertyCondition` | [spec](../superpowers/specs/2026-07-09-conditional-design.md) / [plan](../superpowers/plans/2026-07-09-conditional.md) |
| 7-3 | 国际化 | `i18n/` | `MessageSource`/`NoSuchMessageException`/`AbstractMessageSource`、`StaticMessageSource`/`ResourceBundleMessageSource` | [spec](../superpowers/specs/2026-07-09-messagesource-design.md) / [plan](../superpowers/plans/2026-07-09-messagesource.md) |
| 7-4 | 异步 | `async/` | `@Async`、`AsyncInterceptor` | [spec](../superpowers/specs/2026-07-09-async-design.md) / [plan](../superpowers/plans/2026-07-09-async.md) |
| 7-5 | 事务 | `transaction/` | `@Transactional`/`Propagation`/`TransactionStatus`/`PlatformTransactionManager`、`SimpleTransactionManager`、`TransactionInterceptor` | [spec](../superpowers/specs/2026-07-09-transactional-design.md) / [plan](../superpowers/plans/2026-07-09-transactional.md) |

### 2.1 事件机制（7-1）

发布-订阅：以 `ApplicationEventMulticaster` 为事件引擎核心，`DefaultBeanContainer` 实现 `ApplicationEventPublisher` 并把发布委托给它；`ApplicationListenerDetector`（`BeanPostProcessor`）在 Bean 初始化后自动把 `ApplicationListener` 注册进多播器；`GenericTypeResolver` 解析 `ApplicationListener<E>` 的泛型 `E` 实现按类型路由。容器 `refresh()` eager 实例化单例后广播 `ContextRefreshedEvent`，`destroy()` 广播 `ContextClosedEvent`。

### 2.2 条件装配（7-2）

组件扫描时按 `@Conditional` 决定是否注册。`ConditionEvaluator` 递归走元注解链收集所有 `@Conditional`（直接标注或经组合注解间接标注），实例化 `Condition` 求 AND；内置 `@ConditionalOnProperty` 自身被 `@Conditional(OnPropertyCondition.class)` 标注，靠元注解解析被识别——对应 Spring 的 `AnnotatedElementUtils`。仅 `scanComponents` 评估条件，程序式 `registerBean` 不评估。

### 2.3 国际化（7-3）

`AbstractMessageSource` 模板方法基类统一两个 `getMessage` 重载 + `MessageFormat` 参数替换，子类只实现 `resolveCode`。`ResourceBundleMessageSource` 读 classpath `.properties`（复用 `ResourceBundle.getBundle` 原生 locale 回退），`StaticMessageSource` 内存 Map 便于测试。容器经 `resolveDependency` 钩子让 `@Autowired MessageSource` 注入所配置的实例。

### 2.4 异步（7-4）

`@Async` 接口方法经 AOP 代理异步执行。`AsyncInterceptor`（`AroundAdvice`）把 `@Async` 方法提交到 `Executor`：`void` 发后不管（异常 `printStackTrace`），`CompletableFuture` 把内层 CF 的结果经 `whenComplete` 传播到外层 future。**完全复用** `addAdvisor` + `applyAopProxy`，零容器改动。

### 2.5 事务（7-5）

声明式事务经 AOP 驱动。`SimpleTransactionManager` 用 `ThreadLocal` 事务栈 + 事件日志实现 `REQUIRED`（加入）/ `REQUIRES_NEW`（挂起）传播与 `rollbackOnly` 传播；`TransactionInterceptor` 按规则（默认 RuntimeException/Error 回滚、受检异常提交，`rollbackFor`/`noRollbackFor` 覆盖）提交/回滚。逻辑事务（无真实 DB），同样复用 `addAdvisor`。

## 3. 架构与设计主题

跨五项特性反复出现的设计模式与约束：

- **零依赖 + 接口约束**：核心容器无真实 CGLIB（`CglibProxy` 实为 JDK 代理），故 AOP 代理只能基于接口。`@Async`、`@Transactional` 只能作用于**接口方法**（Bean 按接口获取），与阶段 5 的 `IOrderService`/`OrderService` 模式一致。这是零依赖原则的直接结果，文档与样例均明确。
- **AOP 驱动的高级特性**：`@Async`、`@Transactional` 都不引入新容器代码，而是作为 `AroundAdvice` 经 `addAdvisor` 接入，复用阶段 5 的 `ProxyFactory`/`applyAopProxy`。理解了阶段 5 的 AOP，这两项特性的接入就是顺水推舟。
- **`resolveDependency` 注入钩子**：`DefaultBeanContainer.resolveDependency` 对容器内部类型返回容器自身/所持有实例——`ApplicationEventPublisher`/`BeanContainer` 返回 `this`，`MessageSource` 返回 `messageSource` 字段。Bean `@Autowired` 这些类型即可获得发布器/消息源。
- **`BeanPostProcessor` 自动装配**：`ApplicationListenerDetector`（事件）在后处理阶段自动发现监听器，对标 Spring 的同名后处理器。条件装配则在扫描路径（`scanComponents`）由 `ConditionEvaluator` 把关。
- **`ThreadLocal` 绑定**：事件多播器预留 `Executor` 扩展点（异步分发）；事务管理器用 `ThreadLocal` 事务栈实现传播。两者都是 Spring `TransactionSynchronizationManager`/资源绑定思想的简化呈现。
- **可观测优先**：`SimpleTransactionManager` 记录 begin/commit/rollback 事件序列，`SimpleApplicationEventMulticaster` 可设 `ErrorHandler`/`Executor`——让"事务边界""事件分发"这些无形的概念变得可断言、可教学。

## 4. 与真实 Spring 的对应

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `event.ApplicationEvent`/`Listener`/`Publisher`/`Multicaster` | `org.springframework.context.*`（同名） |
| `condition.@Conditional`/`Condition`/`ConditionEvaluator` | `org.springframework.context.annotation.*`（同名） |
| `condition.@ConditionalOnProperty` | `org.springframework.boot.autoconfigure.condition.*`（Spring Boot） |
| `i18n.MessageSource`/`AbstractMessageSource`/`ResourceBundleMessageSource` | `org.springframework.context.*`（同名） |
| `async.@Async`/`AsyncInterceptor` | `org.springframework.scheduling.annotation.*` / `AsyncExecutionInterceptor` |
| `transaction.@Transactional`/`PlatformTransactionManager`/`TransactionInterceptor` | `org.springframework.transaction.*`（同名） |
| `addAdvisor(...)` 手动接入 @Async/@Transactional | `@EnableAsync`/`@EnableTransactionManagement` 自动注册后处理器 |
| `SimpleTransactionManager`（逻辑事务 + 事件可观测） | `DataSourceTransactionManager`（绑真实 Connection）/ `ResourcelessTransactionManager` |

> 教学要点：本框架用"逻辑事务/事件可观测/元注解手解"把 Spring 的关键抽象显式呈现；真实 Spring 把同样的边界逻辑接到了 JDBC、自动代理、`AnnotatedElementUtils` 上。理解了本框架的机制，就能更自如地读 Spring 源码。

## 5. 测试

阶段 7 新增 84 个测试，总数 99 → **183**。各特性测试类：

- 事件（19）：`ApplicationEventTest`、`GenericTypeResolverTest`、`SimpleApplicationEventMulticasterTest`、`LifecycleEventTest`、`ApplicationListenerDetectorTest`、`EventContainerPublishTest`、`EventContainerLifecycleTest`
- 条件装配（16）：`ConditionEvaluatorTest`、`OnPropertyConditionTest`、`ConditionalContainerIntegrationTest`
- 国际化（19）：`AbstractMessageSourceTest`、`StaticMessageSourceTest`、`ResourceBundleMessageSourceTest`、`MessageSourceContainerIntegrationTest`
- 异步（10）：`AsyncInterceptorTest`、`AsyncContainerIntegrationTest`
- 事务（20）：`TransactionAbstractionsTest`、`SimpleTransactionManagerTest`、`TransactionInterceptorTest`、`TransactionalContainerIntegrationTest`

每个特性附一个 `*Demo` 可运行样例（`samples/event|conditional|messagesource|async|transaction`），演示端到端用法。

## 6. 过程亮点

- **TDD + review 闭环多次抓到真实 bug**：
  - 条件装配：规划阶段我写的 `collect` 算法 `seen` 去重会误丢直接 `@Conditional`，实现者 TDD 时 AND 测试失败、定位并修复。
  - 国际化：`ResourceBundle.getBundle(base, locale)` 实测会先把 JVM 默认 locale 作候选（zh 默认请求 fr 命中 `_zh` 而非根），实现者经验性测试抓到、修正测试隔离。
  - 异步：`CompletableFuture` 路径需用 `whenComplete` 链式传播内层 CF 结果（非 `future.complete(proceed())`）——规划时修复；最终评审又抓到 `whenComplete` 的 error 分支未被测试、补 `failedFuture` 用例。
  - **事务（最重要）**：`@Transactional` 暴露了一个潜伏 4 个阶段的 AOP 缺陷——代理 `proceed()` 未解包 `InvocationTargetException`，使异常感知型 advice（事务回滚判定）失效。修复 `ProxyFactory`/`JdkDynamicProxy`/`CglibProxy` 三处（commit `82cd35b`），全量无回归。
- **环境治理**：会话首日修复了本机 `~/.m2/settings.xml` 的 `jdk-1.8` 默认 profile 压制 JDK 17 源码级的问题。
- **工作流模板沉淀**：五项特性共用 brainstorming→spec→plan→TDD 节奏，spec/plan 文档可作后续特性模板（见 `docs/superpowers/`）。

## 7. 相关文档

- 各阶段完成检查清单：`docs/phases/phase01..06-completion-checklist.md`
- 阶段 7 各特性 spec：`docs/superpowers/specs/2026-07-09-*-design.md`
- 阶段 7 各特性实现计划：`docs/superpowers/plans/2026-07-09-*.md`
- 后期增强与拓展方向：[`docs/future-enhancements.md`](../future-enhancements.md)
