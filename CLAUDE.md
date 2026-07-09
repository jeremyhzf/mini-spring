# CLAUDE.md

> 本文件是给 Claude Code（及协作者）的项目级工作指南。全局规则见 `~/.claude/CLAUDE.md`（中文回答、CodeGraph 等）。

## 项目概述

Mini-Spring 是一个**从零手写的 Spring 教学框架**，按阶段渐进式重建 IoC / DI / 生命周期 / 注解驱动 / AOP / MVC / 高级特性，每个阶段都是可独立运行、带完整单元测试的最小实现。目标是让人真正理解 Spring 在背后做了什么，而非追求功能完备。

- **核心容器零第三方依赖**：仅用 Java 反射与标准库；Web 层依赖 Servlet API（`provided`）。
- **单 Maven 模块**，按功能域扁平分包于 `com.minispring` 下。

## 构建与测试

```bash
mvn clean compile                 # 编译
mvn test                          # 全部单元测试（当前 118 个，全绿）
mvn test -Dtest=ClassName         # 运行单个测试类
mvn test -Dtest=ClassName#method  # 运行单个测试方法

# 运行某阶段示例（每阶段一个 *Demo 入口）
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.event.EventDemo"
```

**JDK 17 为硬性要求**（用了 `instanceof` 模式匹配等 16+ 特性）。

> ⚠️ 环境陷阱：若 `mvn` 报错无法编译 `instanceof` 模式匹配，多半是 `~/.m2/settings.xml` 里的 `jdk-1.8` profile 把 source 压成了 1.8。本仓库已在 2026-07-09 将该 profile 的 `activeByDefault` 改为 `false`；若复发，检查同一处。

## 代码与提交约定

- **TDD 是项目节奏**：先写失败测试，再实现，再验证通过。测试与 `src/main` 一一对应，位于 `src/test/java/com/minispring`。
- **全部注释、文档、提交信息使用中文**。
- **提交信息前缀**：`feat(<scope>): ...` / `fix(<scope>): ...` / `docs: ...` / `refactor: ...` / `test: ...`。scope 用功能域（如 `event`、`dispatcher`、`factory`）。
- **提交卫生（重要）**：本仓库**有意跟踪 `.idea/`**，且 IDE 会自动 stage `.idea/*` 变更。做特性提交时用显式 pathspec，避免无关的 `.idea` 噪声混入：
  ```bash
  git add src/main/java/.../Foo.java src/test/java/.../FooTest.java
  git commit -m "feat(x): ..." -- src/main/java/.../Foo.java src/test/java/.../FooTest.java
  ```
- **核心容器不得引入第三方依赖**（仅 JDK；测试用 JUnit 5；Web 用 provided Servlet API）。新增功能域放新的顶层包，如 `com.minispring.event`。

## 测试编写陷阱

- 容器通过反射实例化 Bean 时**不会**对构造器调 `setAccessible(true)`。因此 `com.minispring.factory` 的容器无法跨包实例化**包私有**的测试夹具类——凡是容器要 `getBean` 实例化的嵌套测试类，必须声明为 `public`（如 `public static class MyListener`）。仅由测试自己 `new` 出来再 `registerBeanInstance` 注册的夹具可保持包私有。
- 监听器测试若要验证「按泛型类型路由」，监听器必须用**命名子类或匿名内部类**，不能用 lambda（lambda 不携带泛型签名，运行时解析不到事件类型）。

## 架构与关键集成点

- **`factory/DefaultBeanContainer` 是贯穿所有阶段的核心**——IoC 容器、注解注入、AOP 代理、事件发布都在此交汇。改动它务必跑**全量 `mvn test`** 防回归（它是被最多测试间接依赖的类）。
- 包结构（`src/main/java/com/minispring`）：
  - `factory/`：IoC 容器核心。`BeanContainer`（接口）、`DefaultBeanContainer`（中心实现）、`instantiator/`（构造器/setter 注入）、`dependency/`（依赖解析、循环依赖检测）、`lifecycle/`（`BeanPostProcessor`/`InitializingBean`/`DisposableBean`）、`scope/`（Singleton/Prototype）。
  - `annotation/`（`@Autowired`/`@Qualifier`/`@Value`）、`stereotype/`（`@Component`/`@Service`/`@Repository`/`@Controller`）、`scanner/`（`ClassPathBeanScanner` 组件扫描）、`env/`（`Environment`，`@Value` 占位符）。
  - `aop/`：`Advisor`/`Pointcut`/`MethodMatcher`、`advice/`（Before/After/Around）、`proxy/`（`ProxyFactory`/`JdkDynamicProxy`/`CglibProxy`）、`interceptor/`。
  - `web/`：MVC。`DispatcherServlet`、`HandlerMapping`、`annotation/`（`@RequestMapping` 等）、`view/`（`ViewResolver`）。
  - `event/`：事件机制。`ApplicationEvent`/`ApplicationListener`/`ApplicationEventPublisher`、`ApplicationEventMulticaster`/`SimpleApplicationEventMulticaster`/`ErrorHandler`、`GenericTypeResolver`、`ApplicationListenerDetector`（后处理器，自动注册监听器）、`ContextRefreshedEvent`/`ContextClosedEvent`。
  - `samples/`：每阶段一个 `*Demo` 入口（`ioc`/`di`/`lifecycle`/`annotation`/`aop`/`mvc`/`event`）。

## 阶段进度

| 阶段 | 主题 | 状态 |
|------|------|------|
| 1-6 | Bean 容器 → DI → 生命周期 → 注解 → AOP → MVC | ✅ 已完成 |
| 7-1 | 事件机制（发布-订阅 + 容器生命周期事件） | ✅ 已完成 |
| 7 | 条件装配 `@Conditional` → 国际化 `MessageSource` → 异步 `@Async` → 事务 `@Transactional` | ⏳ 计划中（按此顺序逐个推进） |

## 文档

- `docs/plans/` —— 各阶段实施计划
- `docs/phases/` —— 各阶段完成检查清单
- `docs/superpowers/specs/` 与 `docs/superpowers/plans/` —— 新特性的设计 spec 与实现计划（阶段 7-1 事件机制即经 brainstorming → spec → plan → TDD 实现 流程产出，可作后续特性模板）

## 扩展新特性（阶段 7 余下部分）

建议沿用既有节奏：每个特性独立走 **brainstorming（spec）→ writing-plans（plan）→ subagent-driven / TDD 实现**。事务（`@Transactional`）会复用阶段 5 的 AOP 基础与现有的 `TransactionInterceptor`（当前仅为打印桩，需接入真实事务管理）；异步（`@Async`）可复用事件多播器中预留的 `Executor` 扩展点。
