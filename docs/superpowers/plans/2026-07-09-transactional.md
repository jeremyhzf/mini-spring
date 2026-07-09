# 事务（@Transactional）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 声明式事务 `@Transactional`：经 AOP 拦截由 `PlatformTransactionManager` 开启/提交/回滚逻辑事务，支持 `REQUIRED`/`REQUIRES_NEW` 传播与默认回滚规则 + `rollbackFor`/`noRollbackFor` 自定义。

**Architecture:** 新增 `com.minispring.transaction` 包：`@Transactional`/`Propagation`/`TransactionStatus`/`PlatformTransactionManager` 抽象 + `SimpleTransactionManager`（ThreadLocal 事务栈 + 事件日志，逻辑事务无真实 DB）+ `TransactionInterceptor`（`AroundAdvice`）。用户 `container.addAdvisor(new TransactionInterceptor(manager), @Transactional 匹配器)` 接入，复用 `applyAopProxy`，零容器改动。

**Tech Stack:** Java 17（`java.util.concurrent` 无关；用 `ThreadLocal`/`ArrayDeque`）、JUnit 5、Maven。零新增第三方依赖。

## Global Constraints

- JDK 17+；`maven.compiler.source/target=17`。
- 核心容器**零第三方依赖**：本特性不得向 `pom.xml` 添加任何依赖（无 JDBC/数据源；`SimpleTransactionManager` 为逻辑事务）。
- 新增类型放 `com.minispring.transaction` 包；样例放 `com.minispring.samples.transaction`。
- **零容器改动**：不改 `DefaultBeanContainer` / `ProxyFactory` / `aop.interceptor.TransactionInterceptor` / `pom.xml` / 任何现有类；纯新增包。
- **接口约束（强制，沿用 `@Async`）**：`@Transactional` 只能作用于**接口方法**（核心容器无真实 CGLIB）；Bean 按接口获取/使用。
- 全部代码注释、文档、提交信息使用中文；提交前缀 `feat(transactional): ...` / `test(transactional): ...` / `docs: ...`。
- TDD：每个任务先写失败测试，再实现，再验证通过，最后提交。
- 容器实例化的夹具/样例类必须 `public`（跨包反射实例化）。
- 提交卫生：本仓库跟踪 `.idea/`，IDE 可能自动 stage `.idea/*`——提交时用**显式 pathspec**（`git commit -- <files>`），只提交本任务的文件。
- plain `mvn` works now（settings.xml jdk-1.8 profile 已关闭，JDK 17 source 生效）。

---

## 文件结构

**新增（main）：**
- `src/main/java/com/minispring/transaction/Transactional.java` — `@Transactional` 注解
- `src/main/java/com/minispring/transaction/Propagation.java` — 枚举（REQUIRED/REQUIRES_NEW）
- `src/main/java/com/minispring/transaction/TransactionStatus.java` — 事务状态接口
- `src/main/java/com/minispring/transaction/SimpleTransactionStatus.java` — 默认状态实现（包级可见）
- `src/main/java/com/minispring/transaction/PlatformTransactionManager.java` — 事务管理器接口
- `src/main/java/com/minispring/transaction/SimpleTransactionManager.java` — 默认实现（ThreadLocal 栈 + 事件日志）
- `src/main/java/com/minispring/transaction/TransactionInterceptor.java` — `AroundAdvice`
- `src/main/java/com/minispring/samples/transaction/TransferService.java` — 样例接口
- `src/main/java/com/minispring/samples/transaction/TransferServiceImpl.java` — 样例实现
- `src/main/java/com/minispring/samples/transaction/TransactionalDemo.java` — 入口

**新增（test）：**
- `src/test/java/com/minispring/transaction/TransactionAbstractionsTest.java`
- `src/test/java/com/minispring/transaction/SimpleTransactionManagerTest.java`
- `src/test/java/com/minispring/transaction/TransactionInterceptorTest.java`
- `src/test/java/com/minispring/transaction/AccountService.java` — 集成测试接口
- `src/test/java/com/minispring/transaction/AccountServiceImpl.java` — 集成测试实现
- `src/test/java/com/minispring/transaction/TransactionalContainerIntegrationTest.java`

**修改：**
- `README.md` — 结构树 / 运行命令 / 路线图 / 特性表

**不修改任何现有类**（`DefaultBeanContainer`、`ProxyFactory`、`aop.interceptor.TransactionInterceptor`、`pom.xml` 等一律不动）。

---

## Task 1: 事务抽象（@Transactional / Propagation / TransactionStatus / PlatformTransactionManager）

**Files:**
- Create: `src/main/java/com/minispring/transaction/Transactional.java`
- Create: `src/main/java/com/minispring/transaction/Propagation.java`
- Create: `src/main/java/com/minispring/transaction/TransactionStatus.java`
- Create: `src/main/java/com/minispring/transaction/PlatformTransactionManager.java`
- Test: `src/test/java/com/minispring/transaction/TransactionAbstractionsTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `@Transactional`：`Propagation propagation() default REQUIRED`、`Class<? extends Throwable>[] rollbackFor() default {}`、`Class<? extends Throwable>[] noRollbackFor() default {}`，`@Target({METHOD,TYPE}) @Retention(RUNTIME)`
  - `enum Propagation { REQUIRED, REQUIRES_NEW }`
  - `interface TransactionStatus { boolean isNewTransaction(); boolean isRollbackOnly(); void setRollbackOnly(); boolean isCompleted(); }`
  - `interface PlatformTransactionManager { TransactionStatus getTransaction(Propagation); void commit(TransactionStatus); void rollback(TransactionStatus); }`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/transaction/TransactionAbstractionsTest.java`:

```java
package com.minispring.transaction;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务抽象契约测试：注解默认值、枚举、接口方法存在性
 */
public class TransactionAbstractionsTest {

    public static class Fixture {
        @Transactional
        public void defaultTx() {
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = IllegalStateException.class)
        public void customTx() {
        }
    }

    @Test
    void transactionalDefaultsAreRequiredAndEmpty() throws NoSuchMethodException {
        Transactional t = Fixture.class.getMethod("defaultTx").getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRED, t.propagation(), "默认传播应为 REQUIRED");
        assertEquals(0, t.rollbackFor().length, "默认 rollbackFor 为空");
        assertEquals(0, t.noRollbackFor().length, "默认 noRollbackFor 为空");
    }

    @Test
    void transactionalCustomAttributes() throws NoSuchMethodException {
        Transactional t = Fixture.class.getMethod("customTx").getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, t.propagation());
        assertEquals(IllegalStateException.class, t.rollbackFor()[0]);
    }

    @Test
    void propagationHasRequiredAndRequiresNew() {
        assertTrue(Arrays.asList(Propagation.values()).contains(Propagation.REQUIRED));
        assertTrue(Arrays.asList(Propagation.values()).contains(Propagation.REQUIRES_NEW));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=TransactionAbstractionsTest`
Expected: BUILD FAILURE —— 找不到符号 `Transactional` / `Propagation`

- [ ] **Step 3: 实现 4 个抽象**

`src/main/java/com/minispring/transaction/Transactional.java`:

```java
package com.minispring.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式事务注解
 * 标在接口方法（或 impl 类）上；经 AOP 代理后由 TransactionInterceptor 驱动事务边界。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {

    /** 传播行为，默认 REQUIRED */
    Propagation propagation() default Propagation.REQUIRED;

    /** 触发回滚的异常类型（为空则用默认规则） */
    Class<? extends Throwable>[] rollbackFor() default {};

    /** 不触发回滚的异常类型（覆盖默认/rollbackFor） */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
```

`src/main/java/com/minispring/transaction/Propagation.java`:

```java
package com.minispring.transaction;

/**
 * 事务传播行为
 */
public enum Propagation {
    /** 有则加入、无则新建（默认） */
    REQUIRED,
    /** 总是新建独立事务（挂起外层） */
    REQUIRES_NEW
}
```

`src/main/java/com/minispring/transaction/TransactionStatus.java`:

```java
package com.minispring.transaction;

/**
 * 事务状态
 */
public interface TransactionStatus {

    /** 是否为新建的事务（非加入） */
    boolean isNewTransaction();

    /** 是否仅回滚 */
    boolean isRollbackOnly();

    /** 标记为仅回滚 */
    void setRollbackOnly();

    /** 是否已完成（commit/rollback） */
    boolean isCompleted();
}
```

`src/main/java/com/minispring/transaction/PlatformTransactionManager.java`:

```java
package com.minispring.transaction;

/**
 * 事务管理器抽象
 * 真实实现可对接 JDBC Connection / DataSource；本框架的 SimpleTransactionManager 为逻辑事务。
 */
public interface PlatformTransactionManager {

    /** 按传播行为获取（开启或加入）事务 */
    TransactionStatus getTransaction(Propagation propagation);

    /** 提交事务 */
    void commit(TransactionStatus status);

    /** 回滚事务 */
    void rollback(TransactionStatus status);
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=TransactionAbstractionsTest`
Expected: BUILD SUCCESS，Tests run: 3

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/transaction/Transactional.java \
        src/main/java/com/minispring/transaction/Propagation.java \
        src/main/java/com/minispring/transaction/TransactionStatus.java \
        src/main/java/com/minispring/transaction/PlatformTransactionManager.java \
        src/test/java/com/minispring/transaction/TransactionAbstractionsTest.java
git commit -m "feat(transactional): 新增事务抽象 Transactional/Propagation/TransactionStatus/PlatformTransactionManager" -- \
        src/main/java/com/minispring/transaction/Transactional.java \
        src/main/java/com/minispring/transaction/Propagation.java \
        src/main/java/com/minispring/transaction/TransactionStatus.java \
        src/main/java/com/minispring/transaction/PlatformTransactionManager.java \
        src/test/java/com/minispring/transaction/TransactionAbstractionsTest.java
```

---

## Task 2: SimpleTransactionManager（ThreadLocal 事务栈 + 事件日志）

**Files:**
- Create: `src/main/java/com/minispring/transaction/SimpleTransactionStatus.java`
- Create: `src/main/java/com/minispring/transaction/SimpleTransactionManager.java`
- Test: `src/test/java/com/minispring/transaction/SimpleTransactionManagerTest.java`

**Interfaces:**
- Consumes: `PlatformTransactionManager`、`TransactionStatus`、`Propagation`（Task 1）
- Produces:
  - `SimpleTransactionStatus implements TransactionStatus`（包级可见）：构造 `(boolean newTransaction)`；`markCompleted()`
  - `SimpleTransactionManager implements PlatformTransactionManager`：`getTransaction(Propagation)`、`commit`、`rollback`；`List<String> getEvents()` 返回当前线程事件序列副本（BEGIN/COMMIT/ROLLBACK）

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/transaction/SimpleTransactionManagerTest.java`:

```java
package com.minispring.transaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleTransactionManager 测试：传播、commit/rollback、回滚传播、事件序列
 */
public class SimpleTransactionManagerTest {

    @Test
    void requiredStartsNewWhenNoneActive() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        assertTrue(s.isNewTransaction());
        assertEquals(List.of("BEGIN"), m.getEvents());
    }

    @Test
    void requiredJoinsWhenActive() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        m.getTransaction(Propagation.REQUIRED); // 外层 BEGIN
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        assertFalse(inner.isNewTransaction());
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入不再发 BEGIN");
    }

    @Test
    void requiresNewAlwaysNew() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        m.getTransaction(Propagation.REQUIRED); // 外层 BEGIN
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRES_NEW);
        assertTrue(inner.isNewTransaction());
        assertEquals(List.of("BEGIN", "BEGIN"), m.getEvents());
    }

    @Test
    void commitNewRecordsCommitAndPops() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        m.commit(s);
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents());
        // 提交后栈空，下一次 REQUIRED 应重新新建
        assertTrue(m.getTransaction(Propagation.REQUIRED).isNewTransaction());
    }

    @Test
    void commitJoinedIsNoOp() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        m.commit(inner);
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入事务提交为 no-op");
        assertFalse(outer.isCompleted(), "外层不应被内层提交完成");
    }

    @Test
    void rollbackNewRecordsRollback() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        m.rollback(s);
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void rollbackJoinedMarksOuterRollbackOnly() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        m.rollback(inner);
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入事务回滚不立即记 ROLLBACK");
        assertTrue(outer.isRollbackOnly(), "外层应被标记 rollbackOnly");
        m.commit(outer); // 外层 rollbackOnly → 提交变回滚
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void requiresNewNestedCommitsIndependently() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRES_NEW);
        m.commit(inner);
        m.commit(outer);
        assertEquals(List.of("BEGIN", "BEGIN", "COMMIT", "COMMIT"), m.getEvents());
        assertTrue(outer.isCompleted() && inner.isCompleted());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=SimpleTransactionManagerTest`
Expected: BUILD FAILURE —— 找不到符号 `SimpleTransactionManager` / `SimpleTransactionStatus`

- [ ] **Step 3: 实现**

`src/main/java/com/minispring/transaction/SimpleTransactionStatus.java`:

```java
package com.minispring.transaction;

/**
 * 事务状态默认实现（包级可见，仅 SimpleTransactionManager 使用）
 */
class SimpleTransactionStatus implements TransactionStatus {

    private final boolean newTransaction;
    private boolean rollbackOnly;
    private boolean completed;

    SimpleTransactionStatus(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    @Override
    public boolean isNewTransaction() {
        return newTransaction;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    void markCompleted() {
        if (completed) {
            throw new IllegalStateException("事务已完成，不能重复提交/回滚");
        }
        this.completed = true;
    }
}
```

`src/main/java/com/minispring/transaction/SimpleTransactionManager.java`:

```java
package com.minispring.transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 可观测的简单事务管理器（逻辑事务，无真实 DB）
 *
 * 用 ThreadLocal 维护当前线程的活动事务栈与事件日志：
 * - REQUIRED：栈非空则加入（不发 BEGIN），栈空则新建并入栈（发 BEGIN）
 * - REQUIRES_NEW：总是新建并入栈（发 BEGIN），外层留在栈下方（逻辑挂起）
 * - commit/rollback：isNew 则记 COMMIT/ROLLBACK 并出栈；加入则 no-op（commit）或标记外层 rollbackOnly（rollback）
 */
public class SimpleTransactionManager implements PlatformTransactionManager {

    private final ThreadLocal<Deque<SimpleTransactionStatus>> activeTxs =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<List<String>> events =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        Deque<SimpleTransactionStatus> stack = activeTxs.get();
        if (propagation == Propagation.REQUIRED && !stack.isEmpty()) {
            // 加入现有事务
            return new SimpleTransactionStatus(false);
        }
        // 新建事务（REQUIRED 空栈 或 REQUIRES_NEW）
        SimpleTransactionStatus status = new SimpleTransactionStatus(true);
        stack.push(status);
        record("BEGIN");
        return status;
    }

    @Override
    public void commit(TransactionStatus status) {
        if (status.isRollbackOnly()) {
            doRollback(status);
            return;
        }
        if (status.isNewTransaction()) {
            asStatus(status).markCompleted();
            record("COMMIT");
            pop(status);
        } else {
            // 加入事务：提交由最外层负责，此处 no-op
            asStatus(status).markCompleted();
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        doRollback(status);
    }

    private void doRollback(TransactionStatus status) {
        if (status.isNewTransaction()) {
            asStatus(status).markCompleted();
            record("ROLLBACK");
            pop(status);
        } else {
            // 加入事务：标记栈顶新事务（外层）rollbackOnly
            asStatus(status).markCompleted();
            Deque<SimpleTransactionStatus> stack = activeTxs.get();
            if (!stack.isEmpty()) {
                stack.peek().setRollbackOnly();
            }
        }
    }

    private void pop(TransactionStatus status) {
        Deque<SimpleTransactionStatus> stack = activeTxs.get();
        if (!stack.isEmpty() && stack.peek() == status) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            activeTxs.remove();
        }
    }

    private void record(String event) {
        events.get().add(event);
    }

    private SimpleTransactionStatus asStatus(TransactionStatus status) {
        return (SimpleTransactionStatus) status;
    }

    /**
     * 当前线程的事件序列副本（BEGIN/COMMIT/ROLLBACK），供测试/样例断言
     */
    public List<String> getEvents() {
        return new ArrayList<>(events.get());
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=SimpleTransactionManagerTest`
Expected: BUILD SUCCESS，Tests run: 8

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/transaction/SimpleTransactionStatus.java \
        src/main/java/com/minispring/transaction/SimpleTransactionManager.java \
        src/test/java/com/minispring/transaction/SimpleTransactionManagerTest.java
git commit -m "feat(transactional): 实现 SimpleTransactionManager（ThreadLocal 事务栈 + 事件日志）" -- \
        src/main/java/com/minispring/transaction/SimpleTransactionStatus.java \
        src/main/java/com/minispring/transaction/SimpleTransactionManager.java \
        src/test/java/com/minispring/transaction/SimpleTransactionManagerTest.java
```

---

## Task 3: TransactionInterceptor（AOP 拦截 + 回滚规则）

**Files:**
- Create: `src/main/java/com/minispring/transaction/TransactionInterceptor.java`
- Test: `src/test/java/com/minispring/transaction/TransactionInterceptorTest.java`

**Interfaces:**
- Consumes: `PlatformTransactionManager`、`@Transactional`、`Propagation`（Task 1-2）；`com.minispring.aop.advice.AroundAdvice`（`Object around(MethodInvocation) throws Throwable`）、`MethodInvocation`（`getMethod()`/`getTarget()`/`proceed()`）
- Produces: `TransactionInterceptor(PlatformTransactionManager manager)`；`around`：读 `@Transactional`（先方法后类）→ `manager.getTransaction(prop)` → `proceed` → 按规则 `commit`/`rollback`；非事务方法直接 `proceed`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/transaction/TransactionInterceptorTest.java`:

```java
package com.minispring.transaction;

import com.minispring.aop.advice.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionInterceptor 测试：提交/回滚规则、rollbackFor/noRollbackFor、非事务方法
 */
public class TransactionInterceptorTest {

    public static class Fixture {
        @Transactional
        public String success() {
            return "ok";
        }

        @Transactional
        public String unchecked() {
            throw new RuntimeException("boom");
        }

        @Transactional
        public String checked() throws Exception {
            throw new Exception("checked");
        }

        @Transactional(rollbackFor = Exception.class)
        public String rollbackForChecked() throws Exception {
            throw new Exception("checked-rb");
        }

        @Transactional(noRollbackFor = RuntimeException.class)
        public String noRollbackUnchecked() {
            throw new RuntimeException("no-rb");
        }

        public String notTransactional() {
            return "plain";
        }
    }

    private MethodInvocation invocation(Object target, String name) throws Exception {
        Method method = target.getClass().getMethod(name);
        return new MethodInvocation() {
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public Object getTarget() { return target; }
            @Override public Object proceed() throws Throwable {
                try {
                    return method.invoke(target);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };
    }

    @Test
    void successCommits() throws Throwable {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        Object result = it.around(invocation(new Fixture(), "success"));
        assertEquals("ok", result);
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents());
    }

    @Test
    void uncheckedExceptionRollsBack() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(RuntimeException.class, () -> it.around(invocation(new Fixture(), "unchecked")));
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void checkedExceptionCommitsByDefault() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(Exception.class, () -> it.around(invocation(new Fixture(), "checked")));
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents(), "受检异常默认提交");
    }

    @Test
    void rollbackForOverridesChecked() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(Exception.class, () -> it.around(invocation(new Fixture(), "rollbackForChecked")));
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void noRollbackForSuppressesUnchecked() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(RuntimeException.class, () -> it.around(invocation(new Fixture(), "noRollbackUnchecked")));
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents(), "noRollbackFor 覆盖默认回滚");
    }

    @Test
    void nonTransactionalMethodProceedsWithoutTransaction() throws Throwable {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        Object result = it.around(invocation(new Fixture(), "notTransactional"));
        assertEquals("plain", result);
        assertTrue(m.getEvents().isEmpty(), "非事务方法不应触发任何事务事件");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=TransactionInterceptorTest`
Expected: BUILD FAILURE —— 找不到符号 `TransactionInterceptor`

- [ ] **Step 3: 实现**

`src/main/java/com/minispring/transaction/TransactionInterceptor.java`:

```java
package com.minispring.transaction;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.Method;

/**
 * 事务拦截器
 * 读 @Transactional → 由管理器开启/加入事务 → 执行目标方法 → 按规则提交/回滚。
 * 全限定名 com.minispring.transaction.TransactionInterceptor；
 * 与阶段 5 的 com.minispring.aop.interceptor.TransactionInterceptor（打印桩）同名不同包。
 */
public class TransactionInterceptor implements AroundAdvice {

    private final PlatformTransactionManager manager;

    public TransactionInterceptor(PlatformTransactionManager manager) {
        this.manager = manager;
    }

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional tx = resolveTransactional(method, invocation.getTarget().getClass());
        if (tx == null) {
            return invocation.proceed();
        }

        TransactionStatus status = manager.getTransaction(tx.propagation());
        try {
            Object result = invocation.proceed();
            manager.commit(status);
            return result;
        } catch (Throwable t) {
            if (shouldRollbackOn(tx, t)) {
                manager.rollback(status);
            } else {
                manager.commit(status);
            }
            throw t;
        }
    }

    /** 先方法（接口方法）后类（impl）查找 @Transactional */
    private Transactional resolveTransactional(Method method, Class<?> targetClass) {
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx != null) {
            return tx;
        }
        return targetClass.getAnnotation(Transactional.class);
    }

    /** 回滚判定：noRollbackFor 优先；rollbackFor 非空则只对命中类型回滚；否则默认 RuntimeException/Error 回滚 */
    private boolean shouldRollbackOn(Transactional tx, Throwable t) {
        for (Class<? extends Throwable> noRollback : tx.noRollbackFor()) {
            if (noRollback.isInstance(t)) {
                return false;
            }
        }
        if (tx.rollbackFor().length > 0) {
            for (Class<? extends Throwable> rollback : tx.rollbackFor()) {
                if (rollback.isInstance(t)) {
                    return true;
                }
            }
            return false;
        }
        return (t instanceof RuntimeException || t instanceof Error);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=TransactionInterceptorTest`
Expected: BUILD SUCCESS，Tests run: 6

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/transaction/TransactionInterceptor.java \
        src/test/java/com/minispring/transaction/TransactionInterceptorTest.java
git commit -m "feat(transactional): 实现 TransactionInterceptor（回滚规则 + 事务边界）" -- \
        src/main/java/com/minispring/transaction/TransactionInterceptor.java \
        src/test/java/com/minispring/transaction/TransactionInterceptorTest.java
```

---

## Task 4: 容器集成测试（addAdvisor + 接口代理）

**Files:**
- Create: `src/test/java/com/minispring/transaction/AccountService.java`
- Create: `src/test/java/com/minispring/transaction/AccountServiceImpl.java`
- Create: `src/test/java/com/minispring/transaction/TransactionalContainerIntegrationTest.java`

**Interfaces:**
- Consumes: `TransactionInterceptor`、`SimpleTransactionManager`、`@Transactional`（Task 1-3）；`DefaultBeanContainer.addAdvisor/registerBean/getBean`、`DefaultAdvisor`、`MethodMatcher`（现有）
- Produces: 验证 `addAdvisor(transactionInterceptor, @Transactional 匹配器)` 后，接口 Bean 的 @Transactional 方法经代理驱动事务边界（事件序列正确）

- [ ] **Step 1: 写夹具与测试**

`src/test/java/com/minispring/transaction/AccountService.java`:

```java
package com.minispring.transaction;

/**
 * 集成测试用接口：@Transactional 标在接口方法上
 */
public interface AccountService {

    @Transactional
    void credit(String account, int amount);

    @Transactional
    void creditAndFail(String account, int amount);
}
```

`src/test/java/com/minispring/transaction/AccountServiceImpl.java`（容器实例化，须 public）:

```java
package com.minispring.transaction;

/**
 * AccountService 实现（逻辑事务，无真实 DB 操作）
 */
public class AccountServiceImpl implements AccountService {

    @Override
    public void credit(String account, int amount) {
        // 业务逻辑占位：真实场景下这里会更新账户余额
    }

    @Override
    public void creditAndFail(String account, int amount) {
        throw new RuntimeException("入账失败");
    }
}
```

`src/test/java/com/minispring/transaction/TransactionalContainerIntegrationTest.java`:

```java
package com.minispring.transaction;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Transactional 与容器集成测试：addAdvisor + 接口代理 + 事务事件
 */
public class TransactionalContainerIntegrationTest {

    private AccountService setup(SimpleTransactionManager manager) {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(manager),
                (MethodMatcher) (method, targetClass) ->
                        method.isAnnotationPresent(Transactional.class)
                                || targetClass.isAnnotationPresent(Transactional.class)
        ));
        container.registerBean("accountService", AccountServiceImpl.class);
        return (AccountService) container.getBean("accountService");
    }

    @Test
    void creditCommitsViaProxy() {
        SimpleTransactionManager manager = new SimpleTransactionManager();
        AccountService svc = setup(manager);

        svc.credit("A001", 100);

        assertEquals(List.of("BEGIN", "COMMIT"), manager.getEvents());
    }

    @Test
    void creditAndFailRollsBackViaProxy() {
        SimpleTransactionManager manager = new SimpleTransactionManager();
        AccountService svc = setup(manager);

        assertThrows(RuntimeException.class, () -> svc.creditAndFail("A001", 100));

        assertEquals(List.of("BEGIN", "ROLLBACK"), manager.getEvents());
    }

    @Test
    void beanIsProxied() {
        Object bean = setup(new SimpleTransactionManager());
        assertNotSame(AccountServiceImpl.class, bean.getClass());
        assertInstanceOf(AccountService.class, bean);
    }
}
```

- [ ] **Step 2: 运行测试，确认通过**

Run: `mvn test -Dtest=TransactionalContainerIntegrationTest`
Expected: BUILD SUCCESS，Tests run: 3

> 说明：Task 1-3 已实现事务抽象与拦截器；本任务验证既有 AOP 容器路径（addAdvisor → applyAopProxy 接口代理 → @Transactional 切点匹配 → TransactionInterceptor）端到端成立，故测试应直接通过。若失败，按 BLOCKED 上报（不要改生产代码或弱化断言）。

- [ ] **Step 3: 运行全量测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 4: 提交（显式 pathspec）**

```bash
git add src/test/java/com/minispring/transaction/AccountService.java \
        src/test/java/com/minispring/transaction/AccountServiceImpl.java \
        src/test/java/com/minispring/transaction/TransactionalContainerIntegrationTest.java
git commit -m "test(transactional): 新增 @Transactional 容器集成测试" -- \
        src/test/java/com/minispring/transaction/AccountService.java \
        src/test/java/com/minispring/transaction/AccountServiceImpl.java \
        src/test/java/com/minispring/transaction/TransactionalContainerIntegrationTest.java
```

---

## Task 5: 样例 TransactionalDemo + README 更新

**Files:**
- Create: `src/main/java/com/minispring/samples/transaction/TransferService.java`
- Create: `src/main/java/com/minispring/samples/transaction/TransferServiceImpl.java`
- Create: `src/main/java/com/minispring/samples/transaction/TransactionalDemo.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `@Transactional`、`TransactionInterceptor`、`SimpleTransactionManager`、`DefaultAdvisor`、`MethodMatcher`、`DefaultBeanContainer`（Task 1-4 + 现有）
- Produces: 可运行的 `com.minispring.samples.transaction.TransactionalDemo`

- [ ] **Step 1: 写样例代码**

`src/main/java/com/minispring/samples/transaction/TransferService.java`:

```java
package com.minispring.samples.transaction;

import com.minispring.transaction.Transactional;

/**
 * 转账服务接口：@Transactional 标在接口方法上
 */
public interface TransferService {

    @Transactional
    void transfer(String from, String to, int amount);

    @Transactional
    void transferAndFail(String from, String to, int amount);
}
```

`src/main/java/com/minispring/samples/transaction/TransferServiceImpl.java`（容器实例化，须 public）:

```java
package com.minispring.samples.transaction;

/**
 * 转账服务实现（逻辑事务，无真实 DB）
 */
public class TransferServiceImpl implements TransferService {

    @Override
    public void transfer(String from, String to, int amount) {
        System.out.println("[业务] " + from + " -> " + to + " : " + amount);
    }

    @Override
    public void transferAndFail(String from, String to, int amount) {
        System.out.println("[业务] " + from + " -> " + to + " : " + amount + "（将失败）");
        throw new RuntimeException("转账失败");
    }
}
```

`src/main/java/com/minispring/samples/transaction/TransactionalDemo.java`:

```java
package com.minispring.samples.transaction;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.transaction.SimpleTransactionManager;
import com.minispring.transaction.Transactional;
import com.minispring.transaction.TransactionInterceptor;

/**
 * 阶段7-5 - 声明式事务示例
 * 演示：@Transactional 接口方法经 AOP 代理驱动事务边界（提交/回滚），通过事件日志直观展示
 */
public class TransactionalDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-5：声明式事务 @Transactional 示例 ===\n");

        SimpleTransactionManager manager = new SimpleTransactionManager();

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(manager),
                (MethodMatcher) (method, targetClass) ->
                        method.isAnnotationPresent(Transactional.class)
                                || targetClass.isAnnotationPresent(Transactional.class)
        ));
        container.registerBean("transferService", TransferServiceImpl.class);
        TransferService service = (TransferService) container.getBean("transferService");

        System.out.println("--- 正常转账（提交）---");
        service.transfer("Alice", "Bob", 100);
        System.out.println("事务事件: " + manager.getEvents());

        System.out.println("\n--- 转账失败（回滚）---");
        try {
            service.transferAndFail("Bob", "Carol", 50);
        } catch (RuntimeException e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
        System.out.println("事务事件: " + manager.getEvents());

        System.out.println("\n=== 阶段7-5 示例结束 ===");
    }
}
```

- [ ] **Step 2: 编译并运行样例**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.transaction.TransactionalDemo"`
Expected: 输出包含
```
--- 正常转账（提交）---
[业务] Alice -> Bob : 100
事务事件: [BEGIN, COMMIT]

--- 转账失败（回滚）---
[业务] Bob -> Carol : 50（将失败）
捕获异常: 转账失败
事务事件: [BEGIN, COMMIT, BEGIN, ROLLBACK]
```

- [ ] **Step 3: 更新 README**

**(a) 特性概览表** —— 在 7-4 行之后追加 7-5 行。将：
```
| 7-4 | 异步 | `@Async`（接口方法）、`AsyncInterceptor`（`AroundAdvice`）、`Executor` 异步执行、`void`/`CompletableFuture` 返回 |
```
改为（在其后新增一行）：
```
| 7-4 | 异步 | `@Async`（接口方法）、`AsyncInterceptor`（`AroundAdvice`）、`Executor` 异步执行、`void`/`CompletableFuture` 返回 |
| 7-5 | 事务 | `@Transactional`、`PlatformTransactionManager`/`SimpleTransactionManager`、`TransactionInterceptor`、REQUIRED/REQUIRES_NEW 传播、回滚规则 |
```

**(b) 项目结构 —— 顶层包区** —— 在 `async/` 块之后、`samples/` 之前插入 `transaction/` 块。将：
```
│   │   └── AsyncInterceptor（AroundAdvice，提交 Executor）
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```
改为：
```
│   │   └── AsyncInterceptor（AroundAdvice，提交 Executor）
│   ├── transaction/              # 事务（阶段 7-5）
│   │   ├── Transactional / Propagation / TransactionStatus / PlatformTransactionManager
│   │   ├── SimpleTransactionManager（ThreadLocal 事务栈，逻辑事务）
│   │   └── TransactionInterceptor（AroundAdvice，事务边界）
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```

**(c) 项目结构 —— samples 区** —— 在 `async/` 之后追加 `transaction/`。将：
```
│       └── async/                        # 阶段7-4 AsyncDemo（NotificationService）
```
改为：
```
│       ├── async/                        # 阶段7-4 AsyncDemo（NotificationService）
│       └── transaction/                  # 阶段7-5 TransactionalDemo（TransferService）
```

**(d) 运行示例命令** —— 在阶段7-4 命令之后追加。将：
```
# 阶段7-4 异步
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.async.AsyncDemo"
```
改为：
```
# 阶段7-4 异步
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.async.AsyncDemo"

# 阶段7-5 事务
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.transaction.TransactionalDemo"
```

**(e) 路线图表** —— 在 7-4 行之后插入 7-5 行，并把第 7 行标记为已完成。将：
```
| 7-4 | 异步（@Async / AsyncInterceptor） | ✅ 已完成 |
| 7 | 事务 `@Transactional` | ⏳ 计划中 |
```
改为：
```
| 7-4 | 异步（@Async / AsyncInterceptor） | ✅ 已完成 |
| 7-5 | 事务（@Transactional / 传播 / 回滚规则） | ✅ 已完成 |
```

**(f) 特性概览提示行** —— 将：
```
> 阶段 7 的事件机制、条件装配、国际化与异步已实现；其余高级特性（事务）见 [路线图](#-路线图)，尚未实现。
```
改为：
```
> 阶段 7 全部高级特性（事件、条件装配、国际化、异步、事务）已实现。
```

- [ ] **Step 4: 运行全部测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/samples/transaction/TransferService.java \
        src/main/java/com/minispring/samples/transaction/TransferServiceImpl.java \
        src/main/java/com/minispring/samples/transaction/TransactionalDemo.java \
        README.md
git commit -m "feat(transactional): 新增阶段7-5 事务样例 TransactionalDemo 并更新 README" -- \
        src/main/java/com/minispring/samples/transaction/TransferService.java \
        src/main/java/com/minispring/samples/transaction/TransferServiceImpl.java \
        src/main/java/com/minispring/samples/transaction/TransactionalDemo.java \
        README.md
```

---

## 完成标准

- 全部 5 个任务提交，`mvn test` 全绿（含 Task 1-4 新增测试，无既有测试回归）。
- `TransactionalDemo` 可运行：正常转账事件 `[BEGIN, COMMIT]`；失败转账事件累计 `[BEGIN, COMMIT, BEGIN, ROLLBACK]`。
- `SimpleTransactionManager` 正确实现 REQUIRED 加入 / REQUIRES_NEW 挂起 / rollbackOnly 传播；`TransactionInterceptor` 正确处理默认回滚规则 + `rollbackFor`/`noRollbackFor`。
- 未引入任何新的第三方依赖；未改动 `DefaultBeanContainer` / `ProxyFactory` / `aop.interceptor.TransactionInterceptor` / `pom.xml` / 任何现有类。
