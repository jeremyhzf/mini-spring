# 阶段 7-5：事务（@Transactional）设计文档

> Mini-Spring 阶段 7「高级特性」的第五个（最后一个）子系统。本特性独立 spec → plan → 实现。
> 创建日期：2026-07-09

## 1. 背景与目标

阶段 7-1～7-4（事件、条件装配、国际化、异步）已完成。本 spec 实现声明式事务：标了 `@Transactional` 的方法在调用时由事务管理器开启事务、方法正常返回则提交、按规则抛异常则回滚，并支持嵌套调用的事务传播。

### 目标

- 提供抽象 `PlatformTransactionManager`（`getTransaction` / `commit` / `rollback`）与 `TransactionStatus`
- 提供 `SimpleTransactionManager`（可观测的默认实现，记录 begin/commit/rollback 事件，**不碰真实 DB**）
- 支持传播 `REQUIRED`（默认：有则加入、无则新建）与 `REQUIRES_NEW`（挂起外层、新建独立事务），经 `ThreadLocal` 绑定当前事务
- 支持回滚规则：默认（RuntimeException/Error 回滚、受检异常提交）+ `@Transactional(rollbackFor/noRollbackFor)` 自定义
- 把现有 `TransactionInterceptor`（阶段 5 打印桩）升级思路落地为真正接入管理器的拦截器
- 复用现有 AOP 基础设施（`addAdvisor` + `applyAopProxy`），**零容器代码改动**

### 成功标准

- `@Transactional` 接口方法成功返回 → 事件序列含 `BEGIN`…`COMMIT`
- 抛 `RuntimeException` → `BEGIN`…`ROLLBACK`；抛受检异常 → 默认 `COMMIT`（不回滚）
- `rollbackFor={CheckedEx.class}` + 抛该受检异常 → 回滚；`noRollbackFor` 排除指定异常 → 不回滚
- 嵌套调用：外层 REQUIRED、内层 REQUIRED → 单一事务（内层加入，只一次 `BEGIN`，外层提交）；内层抛异常 → 外层一并回滚
- 嵌套调用：内层 REQUIRES_NEW → 内层独立事务（双 `BEGIN`…`COMMIT`/`ROLLBACK`），不受外层回滚影响
- 零新增第三方依赖；附完整单元测试、集成测试与可运行样例

### 设计约束

- 核心容器保持零外部依赖（**无 JDBC/数据源**；事务是逻辑边界，非真实 DB 提交）
- **接口约束（强制，沿用 `@Async`）**：核心容器无真实 CGLIB，`@Transactional` 只能作用于**接口方法**（Bean 按接口获取/使用）
- 遵循扁平顶层包风格与渐进式、TDD 驱动的项目惯例
- 向后兼容：不改任何现有行为

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| 事务真实度 | 抽象 `PlatformTransactionManager` + 可观测默认实现 `SimpleTransactionManager`（逻辑事务，无真实 DB）；真实 DB 连接留作核心外的可插拔实现 |
| 传播行为 | `REQUIRED`（默认，ThreadLocal 绑定，有则加入无则新建）+ `REQUIRES_NEW`（挂起外层新建独立事务） |
| 回滚规则 | Spring 默认（RuntimeException/Error 回滚、受检异常提交）+ `@Transactional(rollbackFor/noRollbackFor)` 自定义 |
| 接入机制 | 复用 `addAdvisor` + `TransactionInterceptor(manager)` + `@Transactional` 匹配器；零容器改动 |
| 现有桩处理 | 新建 `com.minispring.transaction` 包放真正实现；保留阶段 5 的 `aop.interceptor.TransactionInterceptor` 打印桩与 `AopDemo` 不动（教学递进：阶段 5 预览 → 阶段 7 实现） |

## 3. 组件清单

新增顶层包 **`com.minispring.transaction`**。所有类型均为新增，**不修改任何现有类**（`DefaultBeanContainer`、`ProxyFactory`、`aop.interceptor.TransactionInterceptor`、`pom.xml` 一律不动）。

| 类型 | 角色 | 职责 |
|------|------|------|
| `@Transactional` | 注解 | `@Target({METHOD,TYPE}) @Retention(RUNTIME)`；`Propagation propagation() default REQUIRED`、`Class<? extends Throwable>[] rollbackFor() default {}`、`Class<? extends Throwable>[] noRollbackFor() default {}` |
| `Propagation` | 枚举 | `REQUIRED`、`REQUIRES_NEW` |
| `TransactionStatus` | 接口 | `boolean isNewTransaction()`、`boolean isRollbackOnly()`、`void setRollbackOnly()`、`boolean isCompleted()` |
| `PlatformTransactionManager` | 接口 | `TransactionStatus getTransaction(Propagation propagation)`、`void commit(TransactionStatus status)`、`void rollback(TransactionStatus status)` |
| `SimpleTransactionManager` | 默认实现 | `ThreadLocal<Deque<TransactionStatus>>` 活动事务栈 + `ThreadLocal<List<String>>` 事件日志；实现 REQUIRED 加入 / REQUIRES_NEW 挂起 / commit / rollback / 回滚传播；`List<String> getEvents()` 暴露当前线程事件序列 |
| `TransactionInterceptor` | `AroundAdvice` | 构造 `TransactionInterceptor(PlatformTransactionManager)`；`around`：读 `@Transactional` → `manager.getTransaction(prop)` → `proceed` → 按规则 `commit`/`rollback`；非 `@Transactional` 方法直接 `proceed` |

> 注：与阶段 5 的 `com.minispring.aop.interceptor.TransactionInterceptor`（打印桩）同名不同包。本类的全限定名为 `com.minispring.transaction.TransactionInterceptor`。

## 4. SimpleTransactionManager 语义（核心）

**状态**：
- `ThreadLocal<Deque<TransactionStatus>> activeTxs`：当前线程的活动**新**事务栈（加入的事务不入栈）
- `ThreadLocal<List<String>> events`：当前线程的事件日志（`"BEGIN"`/`"COMMIT"`/`"ROLLBACK"`）

**`getTransaction(propagation)`**：
- `REQUIRED`：栈非空 → 返回 `new SimpleTransactionStatus(false)`（加入，**不发 BEGIN**）；栈空 → 新建 `SimpleTransactionStatus(true)`、入栈、记 `BEGIN`
- `REQUIRES_NEW`：总是新建 `SimpleTransactionStatus(true)`、入栈、记 `BEGIN`（外层留在栈下方＝逻辑挂起，无真实资源需挂起；新事务出栈后外层自动"恢复"）

**`commit(status)`**：
- 若 `status.isRollbackOnly()` → 转走 `rollback` 语义（记 `ROLLBACK`、出栈）
- 若 `status.isNewTransaction()` → 标记完成、记 `COMMIT`、出栈
- 否则（加入）→ 仅标记完成（**no-op**，提交由最外层新事务负责）

**`rollback(status)`**：
- 若 `status.isNewTransaction()` → 标记完成、记 `ROLLBACK`、出栈
- 否则（加入）→ 标记完成，并把栈顶的新事务（外层）置 `setRollbackOnly()`（保证嵌套异常致外层一并回滚）

**栈维护**：`isNew` 事务 commit/rollback 时出栈；栈空时清理 `ThreadLocal`，避免线程复用泄漏。

**可观测**：`getEvents()` 返回当前线程的 `events` 列表副本，供测试断言序列（如 `["BEGIN","BEGIN","COMMIT","COMMIT"]` 表 REQUIRES_NEW 嵌套两次提交）。

## 5. TransactionInterceptor 语义

```
around(invocation):
    method = invocation.getMethod()
    tx = method.getAnnotation(Transactional)          // 接口方法上的 @Transactional
       否则 invocation.getTarget().getClass().getAnnotation(Transactional)   // 类级（impl）
    if (tx == null) return invocation.proceed()        // 非事务方法（防御）

    status = manager.getTransaction(tx.propagation())
    try:
        result = invocation.proceed()
        manager.commit(status)
        return result
    catch (Throwable t):
        if (shouldRollbackOn(tx, t)):
            manager.rollback(status)
        else:
            manager.commit(status)                     // 如受检异常默认提交
        throw t
```

**`shouldRollbackOn(tx, t)`**（回滚判定）：
1. 若 `t` 命中 `noRollbackFor` 任一类型 → **不回滚**
2. 若 `rollbackFor` 非空：命中任一 → 回滚；否则 → 不回滚
3. 否则默认规则：`t instanceof RuntimeException || t instanceof Error` → 回滚；受检异常 → 不回滚

**@Transactional 定位**：先方法（接口方法），再类（impl 类）。受接口约束所限，方法级 `@Transactional` 标在接口方法上（代理看到的 `Method` 即接口方法）。

## 6. 集成点

**零容器改动。** 用户通过：
```java
container.addAdvisor(new DefaultAdvisor(
    new TransactionInterceptor(transactionManager),
    (MethodMatcher) (method, targetClass) ->
        method.isAnnotationPresent(Transactional.class)
        || targetClass.isAnnotationPresent(Transactional.class)
));
```
接入。现有 `DefaultBeanContainer.applyAopProxy`（有 Advisor 时把接口 Bean 包成 JDK 代理）已提供代理路径。`TransactionInterceptor` 与 `AsyncInterceptor` 同为 `AroundAdvice`。

不修改：`DefaultBeanContainer`、`ProxyFactory`、`CglibProxy`、`aop.interceptor.TransactionInterceptor`、`pom.xml`、其余任何现有类。

## 7. 错误处理

- **回滚判定**按 §5 规则；受检异常默认提交、`RuntimeException`/`Error` 默认回滚，`rollbackFor`/`noRollbackFor` 覆盖
- **加入事务抛异常** → 标记外层 `rollbackOnly` → 外层提交时变回滚（原子性）
- **REQUIRES_NEW 内层回滚** → 仅内层回滚，外层不受影响（独立事务）
- **`@Transactional` 标在不可经接口触达的方法上** → 不被代理拦截，按普通方法执行（约束，文档说明）
- **栈泄漏防护**：`isNew` 事务完成时出栈，栈空清 `ThreadLocal`

## 8. 测试策略

### 单元测试

- `SimpleTransactionManagerTest`：
  - `getTransaction(REQUIRED)` 空栈 → `isNew=true`、记 `BEGIN`
  - `getTransaction(REQUIRED)` 有活动事务 → 返回 `isNew=false`（加入，无新 `BEGIN`）
  - `getTransaction(REQUIRES_NEW)` 有活动事务 → 仍 `isNew=true`、记 `BEGIN`
  - `commit` 新事务 → 记 `COMMIT`、出栈；`commit` 加入事务 → no-op
  - `rollback` 新事务 → 记 `ROLLBACK`；`rollback` 加入事务 → 栈顶置 `rollbackOnly`，随后外层 `commit` 变 `ROLLBACK`
  - 事件序列：REQUIRED 嵌套 → `["BEGIN","COMMIT"]`；REQUIRES_NEW 嵌套 → `["BEGIN","BEGIN","COMMIT","COMMIT"]`
- `TransactionInterceptorTest`（用捕获/直接调用，跨方法模拟嵌套可借助管理器事件序列）：
  - 成功 → `BEGIN`/`COMMIT`
  - 抛 `RuntimeException` → `BEGIN`/`ROLLBACK`
  - 抛受检异常 → 默认 `BEGIN`/`COMMIT`
  - `rollbackFor={CheckedEx.class}` + 抛该受检异常 → 回滚
  - `noRollbackFor={RuntimeException.class}` + 抛 `RuntimeException` → 提交
  - 非事务方法 → 直接 proceed（无事件）
  - 传播：REQUIRED 嵌套（加入，单 `BEGIN`）；REQUIRES_NEW 嵌套（双 `BEGIN`）

### 集成测试

- `TransactionalContainerIntegrationTest`：接口（`@Transactional` 方法）+ impl；`addAdvisor(transactionInterceptor, @Transactional 匹配器)`；`getBean` 按接口；调用成功/异常方法，断言 `manager.getEvents()` 的事件序列（提交/回滚/嵌套传播）。

### 样例

`com.minispring.samples.transaction.TransactionalDemo`：接口含成功方法、抛异常方法、嵌套调用（演示 REQUIRED 加入、REQUIRES_NEW 独立）；打印 `manager.getEvents()` 序列，直观展示事务边界与传播。同步更新 README（项目结构 / 运行命令 / 路线图 阶段 7-5）。

## 9. 范围边界（明确不做）

- 真实 DB 连接 / `DataSource` / `Connection` 绑定（`SimpleTransactionManager` 为逻辑事务）
- 其余 5 种传播（SUPPORTS / NOT_SUPPORTED / NEVER / MANDATORY / NESTED）
- 隔离级别、超时、只读
- `TransactionSynchronizationManager` 完整回调（beforeCommit/afterCompletion 等）
- 声明式 `@EnableTransactionManagement` 自动装配（本框架用 `addAdvisor` 手动接入）
- 程序式 `TransactionTemplate` / `@Transactional` 类级作用在接口上（类级仅支持 impl 类）

## 10. 与真实 Spring 的对应关系（教学要点）

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `@Transactional` | `org.springframework.transaction.annotation.Transactional` |
| `Propagation`（REQUIRED/REQUIRES_NEW） | `org.springframework.transaction.annotation.Propagation`（7 种） |
| `TransactionStatus` | 同名 |
| `PlatformTransactionManager` | 同名 |
| `SimpleTransactionManager`（逻辑事务、可观测） | `DataSourceTransactionManager`（绑真实 Connection）/ `ResourcelessTransactionManager` |
| `TransactionInterceptor`（AroundAdvice） | `org.springframework.transaction.interceptor.TransactionInterceptor` |
| `addAdvisor(...)` 手动接入 | `@EnableTransactionManagement` → 自动注册 `TransactionInterceptor` |
| ThreadLocal 事务栈 | `TransactionSynchronizationManager`（ThreadLocal 绑定资源 + 同步） |

> 教学要点：本框架用"逻辑事务 + 事件可观测"讲清事务边界与传播/回滚语义；真实 Spring 的 `DataSourceTransactionManager` 把同样的边界逻辑接到了 JDBC `Connection.setAutoCommit(false)/commit()/rollback()` 上，并通过 `TransactionSynchronizationManager` 用 ThreadLocal 绑定 Connection——理解了本框架的 ThreadLocal 栈，就理解了 Spring 事务的资源绑定本质。
