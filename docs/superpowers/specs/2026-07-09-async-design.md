# 阶段 7-4：异步（@Async）设计文档

> Mini-Spring 阶段 7「高级特性」的第四个子系统。本特性独立 spec → plan → 实现。
> 创建日期：2026-07-09

## 1. 背景与目标

阶段 7-1（事件）、7-2（条件装配）、7-3（国际化）已完成。本 spec 实现方法级异步：标了 `@Async` 的方法在调用时被提交到 `Executor` 异步执行，不阻塞调用线程。

### 目标

- 提供 `@Async` 注解（方法级，标在接口方法上）
- 提供 `AsyncInterceptor`（`AroundAdvice`）：把 `@Async` 方法提交到 `Executor`；支持 `void`（发后不管）与 `CompletableFuture`（返回异步结果）两种返回类型
- 复用现有 AOP 基础设施（`addAdvisor` + `applyAopProxy`），**零容器代码改动**

### 成功标准

- 接口方法标 `@Async`，Bean 按 `addAdvisor(asyncInterceptor, @Async 匹配器)` 配置后，`getBean` 返回的代理调用该方法时**不在调用线程执行**，而是提交到 Executor
- `void` 返回：调用立即返回，方法体在 Executor 任务中执行
- `CompletableFuture` 返回：调用返回一个 CF，方法体在 Executor 任务中执行完毕后 `complete`（异常则 `completeExceptionally`），调用方可 `.get()`/链式处理
- `@Async` 方法返回非 void/CompletableFuture 时抛 `IllegalStateException`
- 零新增第三方依赖；附完整单元测试、集成测试与可运行样例

### 设计约束

- 核心容器保持零外部依赖（仅用 JDK `java.util.concurrent`）
- **接口约束（强制）**：`@Async` 只能作用于**接口方法**——核心容器零依赖、无真实 CGLIB（`CglibProxy` 实为 JDK 代理），代理只能基于接口，与现有 AOP 模式（`IOrderService`/`OrderService`）一致
- 遵循扁平顶层包风格与渐进式、TDD 驱动的项目惯例
- 向后兼容：不改任何现有行为

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| 代理约束 | 接受接口约束：`@Async` 仅作用于接口方法（Bean 按接口类型获取/使用） |
| 接入机制 | 复用 `addAdvisor` + `AsyncInterceptor`（`AroundAdvice`），与现有 `LoggingInterceptor` 等同族；零容器代码改动 |
| 返回类型 | `void`（发后不管）+ `CompletableFuture`（异步结果） |
| Executor | `AsyncInterceptor(Executor)` 可配；无参构造用默认（守护线程的 `Executors.newCachedThreadPool`） |
| @Async 检查 | `AsyncInterceptor` 防御性地检查方法是否标 `@Async`；`addAdvisor` 的 MethodMatcher 同时按 `@Async` 过滤（仅拦截 @Async 方法） |

## 3. 组件清单

新增顶层包 **`com.minispring.async`**。所有类型均为新增，**不修改任何现有类**（`DefaultBeanContainer`、`ProxyFactory` 等一律不动）。

| 类型 | 角色 | 职责 |
|------|------|------|
| `@Async` | 注解 | `@Target(METHOD) @Retention(RUNTIME)`；标在接口方法上 |
| `AsyncInterceptor` | `AroundAdvice` | 构造可传 `Executor`（默认守护线程缓存池）；`around(invocation)`：若方法无 `@Async` 则直接 `proceed`；否则按返回类型分支——`void` 提交任务（任务内捕获异常 `printStackTrace`）返回 null；`CompletableFuture` 提交任务、完成后 `complete`/`completeExceptionally`；其他返回类型抛 `IllegalStateException` |

> `AsyncInterceptor` 实现 `com.minispring.aop.advice.AroundAdvice`（`Object around(MethodInvocation invocation) throws Throwable`），与 `aop/interceptor/` 下既有拦截器同族。放 `async` 包是为特性内聚（与 `event/`/`condition/` 一致）。

## 4. 数据流

```
用户配置：
  container.addAdvisor(new DefaultAdvisor(
      new AsyncInterceptor(executor),
      (method, targetClass) -> method.isAnnotationPresent(Async.class)));

scanComponents(pkg) → 注册组件 Bean
getBean("mailService")
  → applyAopProxy（容器已有 Advisor）把该接口 Bean 包成 JDK 代理（实现其接口）
  → 调用 ((MailService) proxy).send("x")
  → 代理 invoke → 该方法命中 async Advisor（@Async）
  → AsyncInterceptor.around(invocation)：
       方法返回 void    → executor.execute(() -> { try { invocation.proceed(); } catch (Throwable t) { t.printStackTrace(); } }); return null;
       方法返回 CF      → CompletableFuture<Object> f = new CompletableFuture<>(); executor.execute(() -> { try { f.complete(invocation.proceed()); } catch (Throwable t) { f.completeExceptionally(t); } }); return f;
```

## 5. 集成点

**零容器改动。** `AsyncInterceptor` 是一个 `AroundAdvice`，用户通过 `container.addAdvisor(new DefaultAdvisor(asyncInterceptor, @Async 匹配器))` 接入。现有 `DefaultBeanContainer.applyAopProxy`（容器在有 Advisor 时把接口 Bean 包成 JDK 代理并应用匹配的 Advisor）已提供所需的代理路径，无需新增任何方法或字段。

不修改：`DefaultBeanContainer`、`ProxyFactory`、`CglibProxy`、`pom.xml`、其余任何现有类。

## 6. 错误处理

- **`@Async` 方法返回非 void/CompletableFuture**：`AsyncInterceptor.around` 抛 `IllegalStateException`（"异步方法必须返回 void 或 CompletableFuture"），明确反馈误用
- **`void` 异步方法在任务中抛异常**：任务内捕获并 `printStackTrace()`（不静默吞；对标 Spring 默认未捕获行为）
- **`CompletableFuture` 异步方法在任务中抛异常**：`future.completeExceptionally(t)`，调用方可通过 `.get()`/`exceptionally` 感知
- **`@Async` 标在不可经接口触达的方法上**（如具体类独有方法）：不会被代理拦截，按普通同步方法执行（约束，须在文档/样例说明）
- **`AsyncInterceptor` 被用于无 `@Async` 的方法**（防御性）：直接 `invocation.proceed()`，同步执行

## 7. 测试策略

### 单元测试

- `AsyncInterceptorTest`：用**捕获型 Executor**（`Runnable::add` 收集任务，不内联执行）保证确定性
  - `void` 返回：`around` 立即返回 null；方法体在捕获的 Runnable 运行后才执行（断言运行前状态 vs 运行后状态）
  - `CompletableFuture` 返回：`around` 返回未完成的 CF；运行捕获任务后 CF `complete` 为预期结果；`.get()` 取到值
  - CF 方法任务抛异常：CF `completeExceptionally`（`.get()` 抛 `ExecutionException`）
  - 返回非 void/CF：抛 `IllegalStateException`
  - 防御性：对无 `@Async` 的方法直接 proceed（同步执行，不提交 Executor）
  - `void` 任务内抛异常：`printStackTrace` 不向外传播（任务 Runnable 正常返回）

### 集成测试

- `AsyncContainerIntegrationTest`：
  - 接口 `MailService { @Async void send(String to); @Async CompletableFuture<String> fetch(String q); }` + impl `MailServiceImpl`（容器实例化，须 public）
  - `container.addAdvisor(new DefaultAdvisor(new AsyncInterceptor(capturingExecutor), @Async 匹配器))`；`registerBean`；`getBean` 按接口
  - 验证：`send` 调用后任务在捕获 Executor 中（调用线程未执行方法体）；`fetch` 返回的 CF 在任务运行后拿到结果
  - 用捕获型 Executor 保证确定性（运行捕获任务再断言）

### 样例

`com.minispring.samples.async.AsyncDemo`：接口 `NotificationService { @Async void notify(String msg); @Async CompletableFuture<String> prepare(String msg); }` + impl；真实默认 Executor（守护线程池）演示：`notify` 发后不管（主线程不阻塞）、`prepare` 返回 CF、`.thenAccept`/`.get()` 取结果。同步更新 README（项目结构 / 运行命令 / 路线图 阶段 7-4）。

## 8. 范围边界（明确不做）

- 具体类 `@Async`（接口约束，需真实 CGLIB）
- `@Async("executorName")` 指定具名 Executor
- 自定义 `AsyncUncaughtExceptionHandler`（默认 `printStackTrace`）
- `Future` / `ListenableFuture` 返回类型（仅 `void` + `CompletableFuture`）
- 类级 `@Async`（所有方法异步）
- 专用 `AsyncAnnotationBeanPostProcessor`（复用 `addAdvisor` 即可，不做自动后处理器）

## 9. 与真实 Spring 的对应关系（教学要点）

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `@Async` | `org.springframework.scheduling.annotation.Async` |
| `AsyncInterceptor`（`AroundAdvice`） | `AsyncExecutionInterceptor`（`AsyncExecutionAspectSupport`） |
| `addAdvisor(asyncInterceptor, @Async 匹配器)` | `AsyncAnnotationBeanPostProcessor` + `AsyncAnnotationAdvisor`（自动装配） |
| 默认守护线程缓存池 | `SimpleAsyncTaskExecutor` / `TaskExecutor` |
| 接口代理约束（无 CGLIB） | Spring 有 CGLIB，可代理具体类；本框架受零依赖所限只支持接口 |

> 教学要点：本框架复用现有 AOP `addAdvisor` 机制手接入 `@Async`，而 Spring 用 `BeanPostProcessor` 自动装配——对比可看出"注解驱动 + 自动代理"在 Spring 中是如何由后处理器完成的。
