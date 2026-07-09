# 条件装配（@Conditional）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 组件扫描时根据 `@Conditional` 决定是否把候选类注册为 Bean，并提供内置的 `@ConditionalOnProperty` 按 Environment 属性开关 Bean。

**Architecture:** 新增 `com.minispring.condition` 包；`ConditionEvaluator` 负责元注解解析 + Condition 实例化 + 求值（AND 语义）；`DefaultBeanContainer.scanComponents` 在 `registerBean` 之前委托它判断。`@ConditionalOnProperty` 自身被 `@Conditional(OnPropertyCondition.class)` 标注，靠元注解解析被识别。

**Tech Stack:** Java 17（反射 + 注解元模型）、JUnit 5、Maven。零新增第三方依赖。

## Global Constraints

- JDK 17+；`maven.compiler.source/target=17`。
- 核心容器**零第三方依赖**：本特性不得向 `pom.xml` 添加任何依赖（仅用 JDK 自带类）。
- 新增类型放 `com.minispring.condition` 包；样例放 `com.minispring.samples.conditional`。
- 全部代码注释、文档、提交信息使用中文；提交前缀 `feat(conditional): ...` / `docs: ...`。
- TDD：每个任务先写失败测试，再实现，再验证通过，最后提交。
- 条件评估**只在 `scanComponents` 生效**；程序式 `registerBean(name, class)` 不评估条件（对标 Spring）。
- 容器通过反射实例化 Bean 时**不**对构造器调 `setAccessible`，故**容器实例化的夹具/样例类必须 `public`**（跨包可见）。Condition 实现类由 `ConditionEvaluator` 实例化（内部已 `setAccessible`），无需 public。
- 本机 `~/.m2/settings.xml` 的 jdk-1.8 默认 profile 已关闭，普通 `mvn` 命令即可编译 JDK 17。
- 提交卫生：本仓库跟踪 `.idea/`，IDE 可能自动 stage `.idea/*`——提交时用**显式 pathspec**（`git commit -- <files>`）避免混入。

---

## 文件结构

**新增（main）：**
- `src/main/java/com/minispring/condition/Conditional.java` — `@Conditional` 注解
- `src/main/java/com/minispring/condition/Condition.java` — 条件接口
- `src/main/java/com/minispring/condition/ConditionContext.java` — 上下文（Environment + 候选类）
- `src/main/java/com/minispring/condition/ConditionEvaluator.java` — 求值器（元注解解析 + 实例化 + 求值）
- `src/main/java/com/minispring/condition/ConditionalOnProperty.java` — 内置便捷注解
- `src/main/java/com/minispring/condition/OnPropertyCondition.java` — 属性条件实现

**新增（samples）：**
- `src/main/java/com/minispring/samples/conditional/BasicService.java`
- `src/main/java/com/minispring/samples/conditional/PremiumService.java`
- `src/main/java/com/minispring/samples/conditional/ConditionalDemo.java`

**新增（test）：**
- `src/test/java/com/minispring/condition/ConditionEvaluatorTest.java`
- `src/test/java/com/minispring/condition/OnPropertyConditionTest.java`
- `src/test/java/com/minispring/condition/test/AlwaysService.java`（集成测试夹具）
- `src/test/java/com/minispring/condition/test/FeatureEnabledService.java`（集成测试夹具）
- `src/test/java/com/minispring/condition/ConditionalContainerIntegrationTest.java`

**修改：**
- `src/main/java/com/minispring/factory/DefaultBeanContainer.java` — 仅 `scanComponents` 方法
- `README.md` — 结构树 / 运行命令 / 路线图 / 特性表

---

## Task 1: 条件引擎核心（@Conditional / Condition / ConditionContext / ConditionEvaluator）

**Files:**
- Create: `src/main/java/com/minispring/condition/Conditional.java`
- Create: `src/main/java/com/minispring/condition/Condition.java`
- Create: `src/main/java/com/minispring/condition/ConditionContext.java`
- Create: `src/main/java/com/minispring/condition/ConditionEvaluator.java`
- Test: `src/test/java/com/minispring/condition/ConditionEvaluatorTest.java`

**Interfaces:**
- Consumes: `com.minispring.env.Environment`（现有：`String getProperty(String key)`）
- Produces:
  - `@Conditional` 注解：`Class<? extends Condition> value()`，`@Target(TYPE) @Retention(RUNTIME)`
  - `interface Condition { boolean matches(ConditionContext context); }`
  - `class ConditionContext`：`ConditionContext(Environment, Class<?>)`、`Environment getEnvironment()`、`Class<?> getCandidate()`
  - `class ConditionEvaluator`：`ConditionEvaluator(Environment environment)`、`boolean shouldRegister(Class<?> candidate)`；无 `@Conditional` 返回 true；多个可达 Condition 全真才 true（AND）

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/condition/ConditionEvaluatorTest.java`:

```java
package com.minispring.condition;

import com.minispring.env.Environment;
import com.minispring.env.StandardEnvironment;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件求值器测试：无注解、直接标注、元注解解析、AND 语义
 */
public class ConditionEvaluatorTest {

    /** 恒真条件 */
    public static class TrueCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            return true;
        }
    }

    /** 恒假条件 */
    public static class FalseCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            return false;
        }
    }

    /** 组合注解：自身携带 @Conditional(TrueCondition.class) */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Conditional(TrueCondition.class)
    public @interface ComposedTrue {
    }

    static class NoCondition {
    }

    @Conditional(TrueCondition.class)
    static class DirectTrue {
    }

    @Conditional(FalseCondition.class)
    static class DirectFalse {
    }

    @ComposedTrue
    static class MetaTrue {
    }

    /** 经组合注解带 TrueCondition，又直接标 FalseCondition —— AND 应为 false */
    @ComposedTrue
    @Conditional(FalseCondition.class)
    static class AndCase {
    }

    private ConditionEvaluator evaluator() {
        Environment env = new StandardEnvironment();
        return new ConditionEvaluator(env);
    }

    @Test
    void shouldRegisterWhenNoConditional() {
        assertTrue(evaluator().shouldRegister(NoCondition.class), "无 @Conditional 应注册");
    }

    @Test
    void shouldRegisterWhenDirectConditionTrue() {
        assertTrue(evaluator().shouldRegister(DirectTrue.class));
    }

    @Test
    void shouldSkipWhenDirectConditionFalse() {
        assertFalse(evaluator().shouldRegister(DirectFalse.class));
    }

    @Test
    void shouldResolveConditionViaMetaAnnotation() {
        assertTrue(evaluator().shouldRegister(MetaTrue.class),
            "应透过组合注解 @ComposedTrue 解析到 @Conditional(TrueCondition)");
    }

    @Test
    void shouldAndMultipleReachableConditions() {
        assertFalse(evaluator().shouldRegister(AndCase.class),
            "多个可达 Condition 取 AND，其一为 false 则跳过");
    }

    @Test
    void conditionContextShouldExposeEnvironmentAndCandidate() {
        Environment env = new StandardEnvironment();
        ConditionContext ctx = new ConditionContext(env, DirectTrue.class);
        assertTrue(ctx.getEnvironment() == env, "应暴露构造时传入的 Environment");
        assertTrue(ctx.getCandidate() == DirectTrue.class, "应暴露候选类");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=ConditionEvaluatorTest`
Expected: BUILD FAILURE —— 找不到符号 `Conditional` / `Condition` / `ConditionContext` / `ConditionEvaluator`

- [ ] **Step 3: 实现核心引擎（4 个文件）**

`src/main/java/com/minispring/condition/Conditional.java`:

```java
package com.minispring.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件装配注解
 * 标注在组件类上；扫描时由 ConditionEvaluator 求解其 Condition，
 * 全部 matches 为 true 才注册该 Bean。可经组合注解间接标注（元注解）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Conditional {

    /**
     * 条件实现类（需有无参构造）
     */
    Class<? extends Condition> value();
}
```

`src/main/java/com/minispring/condition/Condition.java`:

```java
package com.minispring.condition;

/**
 * 条件接口
 * 实现类在 ConditionContext 上判断是否应当注册候选 Bean。
 */
public interface Condition {

    /**
     * 是否满足条件
     *
     * @param context 条件上下文（Environment + 候选类）
     * @return true 表示满足，候选 Bean 应被注册
     */
    boolean matches(ConditionContext context);
}
```

`src/main/java/com/minispring/condition/ConditionContext.java`:

```java
package com.minispring.condition;

import com.minispring.env.Environment;

/**
 * 条件求值上下文
 * 向 Condition 暴露 Environment 与被评估的候选类。
 */
public class ConditionContext {

    private final Environment environment;
    private final Class<?> candidate;

    public ConditionContext(Environment environment, Class<?> candidate) {
        this.environment = environment;
        this.candidate = candidate;
    }

    /**
     * 环境（用于按属性判断等）
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * 被评估的候选 Bean 类
     */
    public Class<?> getCandidate() {
        return candidate;
    }
}
```

`src/main/java/com/minispring/condition/ConditionEvaluator.java`:

```java
package com.minispring.condition;

import com.minispring.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 条件求值器
 * 解析候选类上（含经组合注解间接标注的）所有 @Conditional，
 * 实例化其 Condition 并求 AND；全真才应注册。无任何 @Conditional 时直接注册。
 */
public class ConditionEvaluator {

    private final Environment environment;

    public ConditionEvaluator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 候选类是否应当注册
     */
    public boolean shouldRegister(Class<?> candidate) {
        Set<Class<? extends Condition>> conditionTypes = findConditions(candidate);
        if (conditionTypes.isEmpty()) {
            return true;
        }
        ConditionContext context = new ConditionContext(environment, candidate);
        for (Class<? extends Condition> type : conditionTypes) {
            Condition condition = instantiate(type);
            if (!condition.matches(context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 递归收集候选类上所有可达 @Conditional 的 Condition 类型（含元注解链）
     */
    private Set<Class<? extends Condition>> findConditions(Class<?> candidate) {
        Set<Class<? extends Condition>> result = new LinkedHashSet<>();
        Set<Class<? extends Annotation>> seen = new HashSet<>();
        collect(candidate, result, seen);
        return result;
    }

    private void collect(AnnotatedElement element,
                         Set<Class<? extends Condition>> result,
                         Set<Class<? extends Annotation>> seen) {
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (!seen.add(type)) {
                continue;
            }
            if (type == Conditional.class) {
                // 直接标注的 @Conditional
                result.add(((Conditional) annotation).value());
            } else {
                // 间接：该注解类型上是否元标注了 @Conditional
                Conditional meta = type.getDeclaredAnnotation(Conditional.class);
                if (meta != null) {
                    result.add(meta.value());
                }
                // 继续向上递归，处理更深的组合注解
                collect(type, result, seen);
            }
        }
    }

    private Condition instantiate(Class<? extends Condition> type) {
        try {
            Constructor<? extends Condition> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("无法实例化 Condition: " + type.getName(), e);
        }
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=ConditionEvaluatorTest`
Expected: BUILD SUCCESS，Tests run: 6

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/condition/Conditional.java \
        src/main/java/com/minispring/condition/Condition.java \
        src/main/java/com/minispring/condition/ConditionContext.java \
        src/main/java/com/minispring/condition/ConditionEvaluator.java \
        src/test/java/com/minispring/condition/ConditionEvaluatorTest.java
git commit -m "feat(conditional): 实现条件引擎核心 ConditionEvaluator" -- \
        src/main/java/com/minispring/condition/Conditional.java \
        src/main/java/com/minispring/condition/Condition.java \
        src/main/java/com/minispring/condition/ConditionContext.java \
        src/main/java/com/minispring/condition/ConditionEvaluator.java \
        src/test/java/com/minispring/condition/ConditionEvaluatorTest.java
```

---

## Task 2: 内置 @ConditionalOnProperty + OnPropertyCondition

**Files:**
- Create: `src/main/java/com/minispring/condition/ConditionalOnProperty.java`
- Create: `src/main/java/com/minispring/condition/OnPropertyCondition.java`
- Test: `src/test/java/com/minispring/condition/OnPropertyConditionTest.java`

**Interfaces:**
- Consumes: `@Conditional`、`Condition`、`ConditionContext`、`Environment.getProperty`（Task 1 + 现有）
- Produces:
  - `@ConditionalOnProperty` 注解：`@Conditional(OnPropertyCondition.class)`；属性 `String name()`、`String havingValue() default ""`、`boolean matchIfMissing() default false`
  - `OnPropertyCondition implements Condition`：从候选类读直接标注的 `@ConditionalOnProperty`，按 Environment 属性判断

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/condition/OnPropertyConditionTest.java`:

```java
package com.minispring.condition;

import com.minispring.env.StandardEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ConditionalOnProperty 属性匹配测试
 */
public class OnPropertyConditionTest {

    @ConditionalOnProperty(name = "feature.x", havingValue = "true")
    static class MatchByValue {
    }

    @ConditionalOnProperty(name = "feature.x")  // havingValue 为空：只要属性存在即匹配
    static class MatchByPresence {
    }

    @ConditionalOnProperty(name = "feature.x", matchIfMissing = true)
    static class MatchIfMissing {
    }

    @ConditionalOnProperty(name = "feature.x", havingValue = "true")
    static class MissingNoFallback {
    }

    private boolean evaluate(Class<?> candidate, String propertyValue) {
        StandardEnvironment env = new StandardEnvironment();
        if (propertyValue != null) {
            env.setProperty("feature.x", propertyValue);
        }
        ConditionEvaluator evaluator = new ConditionEvaluator(env);
        return evaluator.shouldRegister(candidate);
    }

    @Test
    void shouldMatchWhenValueEquals() {
        assertTrue(evaluate(MatchByValue.class, "true"));
    }

    @Test
    void shouldNotMatchWhenValueDiffers() {
        assertFalse(evaluate(MatchByValue.class, "false"));
    }

    @Test
    void shouldMatchByPresenceWhenHavingValueEmpty() {
        assertTrue(evaluate(MatchByPresence.class, "anything"));
    }

    @Test
    void shouldNotMatchWhenAbsentAndNoFallback() {
        assertFalse(evaluate(MissingNoFallback.class, null));
    }

    @Test
    void shouldMatchWhenAbsentAndMatchIfMissing() {
        assertTrue(evaluate(MatchIfMissing.class, null));
    }

    @Test
    void shouldNotMatchByPresenceWhenAbsent() {
        assertFalse(evaluate(MatchByPresence.class, null));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=OnPropertyConditionTest`
Expected: BUILD FAILURE —— 找不到符号 `ConditionalOnProperty`（`OnPropertyCondition` 被其引用，一并缺失）

- [ ] **Step 3: 实现**

`src/main/java/com/minispring/condition/ConditionalOnProperty.java`:

```java
package com.minispring.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内置条件：按 Environment 属性开关 Bean。
 * 自身被 @Conditional(OnPropertyCondition.class) 标注，
 * 靠 ConditionEvaluator 的元注解解析被识别。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
public @interface ConditionalOnProperty {

    /**
     * 属性名
     */
    String name();

    /**
     * 期望的属性值；为空表示只要属性存在即匹配
     */
    String havingValue() default "";

    /**
     * 属性缺失时是否视为匹配
     */
    boolean matchIfMissing() default false;
}
```

`src/main/java/com/minispring/condition/OnPropertyCondition.java`:

```java
package com.minispring.condition;

/**
 * @ConditionalOnProperty 的条件实现
 * 从候选类读取直接标注的 @ConditionalOnProperty，按 Environment 属性判断。
 */
public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context) {
        ConditionalOnProperty annotation = context.getCandidate().getAnnotation(ConditionalOnProperty.class);
        String value = context.getEnvironment().getProperty(annotation.name());

        if (value == null) {
            // 属性缺失
            return annotation.matchIfMissing();
        }
        if (annotation.havingValue().isEmpty()) {
            // 只要属性存在即匹配
            return true;
        }
        return annotation.havingValue().equals(value);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=OnPropertyConditionTest`
Expected: BUILD SUCCESS，Tests run: 6

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/condition/ConditionalOnProperty.java \
        src/main/java/com/minispring/condition/OnPropertyCondition.java \
        src/test/java/com/minispring/condition/OnPropertyConditionTest.java
git commit -m "feat(conditional): 新增内置 @ConditionalOnProperty 条件注解" -- \
        src/main/java/com/minispring/condition/ConditionalOnProperty.java \
        src/main/java/com/minispring/condition/OnPropertyCondition.java \
        src/test/java/com/minispring/condition/OnPropertyConditionTest.java
```

---

## Task 3: 容器集成（scanComponents 接入 ConditionEvaluator）

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`（仅 `scanComponents` 方法）
- Create: `src/test/java/com/minispring/condition/test/AlwaysService.java`
- Create: `src/test/java/com/minispring/condition/test/FeatureEnabledService.java`
- Test: `src/test/java/com/minispring/condition/ConditionalContainerIntegrationTest.java`

**Interfaces:**
- Consumes: `ConditionEvaluator(Environment)` + `shouldRegister(Class<?>)`（Task 1）；`DefaultBeanContainer.getEnvironment()`、`ClassPathBeanScanner.scan()/generateBeanName()`（现有）
- Produces: `DefaultBeanContainer.scanComponents` 现在跳过条件不满足的候选；`registerBean(name, class)` 行为不变（不评估条件）

- [ ] **Step 1: 写集成测试与夹具**

夹具必须是 `public` 顶层类（容器跨包反射实例化），放在专用测试子包 `com.minispring.condition.test`，扫描时只命中这两个。

`src/test/java/com/minispring/condition/test/AlwaysService.java`:

```java
package com.minispring.condition.test;

import com.minispring.stereotype.Service;

/** 无条件组件：扫描时恒注册 */
@Service
public class AlwaysService {
}
```

`src/test/java/com/minispring/condition/test/FeatureEnabledService.java`:

```java
package com.minispring.condition.test;

import com.minispring.condition.ConditionalOnProperty;
import com.minispring.stereotype.Service;

/** 条件组件：仅当 feature.x=true 时注册 */
@Service
@ConditionalOnProperty(name = "feature.x", havingValue = "true")
public class FeatureEnabledService {
}
```

`src/test/java/com/minispring/condition/ConditionalContainerIntegrationTest.java`:

```java
package com.minispring.condition;

import com.minispring.factory.BeanNotFoundException;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 条件装配与容器集成测试：扫描路径评估条件、程序式注册不评估条件
 */
public class ConditionalContainerIntegrationTest {

    @Test
    void shouldRegisterConditionalBeanWhenPropertyMatches() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.getEnvironment().setProperty("feature.x", "true");
        container.scanComponents("com.minispring.condition.test");

        assertDoesNotThrow(() -> container.getBean("alwaysService"));
        assertDoesNotThrow(() -> container.getBean("featureEnabledService"));
    }

    @Test
    void shouldSkipConditionalBeanWhenPropertyAbsent() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.scanComponents("com.minispring.condition.test");

        assertDoesNotThrow(() -> container.getBean("alwaysService"));
        assertThrows(BeanNotFoundException.class, () -> container.getBean("featureEnabledService"));
    }

    @Test
    void shouldSkipConditionalBeanWhenPropertyMismatch() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.getEnvironment().setProperty("feature.x", "false");
        container.scanComponents("com.minispring.condition.test");

        assertThrows(BeanNotFoundException.class, () -> container.getBean("featureEnabledService"));
        assertDoesNotThrow(() -> container.getBean("alwaysService"));
    }

    @Test
    void registerBeanShouldIgnoreConditions() {
        // 程序式注册即便类上有 @ConditionalOnProperty 也应注册
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.registerBean("manual",
                com.minispring.condition.test.FeatureEnabledService.class);
        assertDoesNotThrow(() -> container.getBean("manual"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=ConditionalContainerIntegrationTest`
Expected: `shouldSkipConditionalBeanWhenPropertyAbsent` / `...Mismatch` 失败——条件 Bean 未被跳过（`getBean` 不抛异常），因为 `scanComponents` 尚未接入 `ConditionEvaluator`

- [ ] **Step 3: 修改 DefaultBeanContainer.scanComponents**

将 `src/main/java/com/minispring/factory/DefaultBeanContainer.java` 中现有的 `scanComponents`：

```java
    public int scanComponents(String basePackage) {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner(basePackage);
        Set<Class<?>> components = scanner.scan();

        int count = 0;
        for (Class<?> component : components) {
            String beanName = scanner.generateBeanName(component);
            registerBean(beanName, component);
            count++;
        }

        return count;
    }
```

改为（注册前用 `ConditionEvaluator` 判断）：

```java
    public int scanComponents(String basePackage) {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner(basePackage);
        Set<Class<?>> components = scanner.scan();

        com.minispring.condition.ConditionEvaluator evaluator =
                new com.minispring.condition.ConditionEvaluator(getEnvironment());

        int count = 0;
        for (Class<?> component : components) {
            if (!evaluator.shouldRegister(component)) {
                continue;   // 条件不满足，跳过注册
            }
            String beanName = scanner.generateBeanName(component);
            registerBean(beanName, component);
            count++;
        }

        return count;
    }
```

不改动 `registerBean(String, Class<?>)` 及其他任何方法。

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=ConditionalContainerIntegrationTest`
Expected: BUILD SUCCESS，Tests run: 4

- [ ] **Step 5: 运行全量测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过（既有 scanner/AOP/事件测试不受影响）

- [ ] **Step 6: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/factory/DefaultBeanContainer.java \
        src/test/java/com/minispring/condition/test/AlwaysService.java \
        src/test/java/com/minispring/condition/test/FeatureEnabledService.java \
        src/test/java/com/minispring/condition/ConditionalContainerIntegrationTest.java
git commit -m "feat(conditional): scanComponents 接入 ConditionEvaluator 跳过不满足条件的 Bean" -- \
        src/main/java/com/minispring/factory/DefaultBeanContainer.java \
        src/test/java/com/minispring/condition/test/AlwaysService.java \
        src/test/java/com/minispring/condition/test/FeatureEnabledService.java \
        src/test/java/com/minispring/condition/ConditionalContainerIntegrationTest.java
```

---

## Task 4: 样例 ConditionalDemo + README 更新

**Files:**
- Create: `src/main/java/com/minispring/samples/conditional/BasicService.java`
- Create: `src/main/java/com/minispring/samples/conditional/PremiumService.java`
- Create: `src/main/java/com/minispring/samples/conditional/ConditionalDemo.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `@ConditionalOnProperty`、`@Service`、`DefaultBeanContainer.scanComponents/getEnvironment/getBean`（Task 1-3 + 现有）
- Produces: 可运行的 `com.minispring.samples.conditional.ConditionalDemo` 入口

- [ ] **Step 1: 编写样例代码**

`src/main/java/com/minispring/samples/conditional/BasicService.java`:

```java
package com.minispring.samples.conditional;

import com.minispring.stereotype.Service;

/**
 * 常驻服务：无条件，扫描时恒注册
 */
@Service
public class BasicService {

    public String serve() {
        return "基础服务";
    }
}
```

`src/main/java/com/minispring/samples/conditional/PremiumService.java`:

```java
package com.minispring.samples.conditional;

import com.minispring.condition.ConditionalOnProperty;
import com.minispring.stereotype.Service;

/**
 * 高级服务：仅当 feature.premium=true 时注册
 */
@Service
@ConditionalOnProperty(name = "feature.premium", havingValue = "true")
public class PremiumService {

    public String serve() {
        return "高级服务";
    }
}
```

`src/main/java/com/minispring/samples/conditional/ConditionalDemo.java`:

```java
package com.minispring.samples.conditional;

import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段7-2 - 条件装配示例
 * 演示：@ConditionalOnProperty 按环境属性开关 Bean
 */
public class ConditionalDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-2：条件装配示例 ===\n");

        // 场景1：未设置属性 → 条件 Bean 不注册
        System.out.println("--- 场景1：未启用 premium（feature.premium 未设置）---");
        DefaultBeanContainer c1 = new DefaultBeanContainer();
        c1.scanComponents("com.minispring.samples.conditional");
        report(c1);

        // 场景2：设置属性 → 条件 Bean 注册
        System.out.println("\n--- 场景2：启用 premium（feature.premium=true）---");
        DefaultBeanContainer c2 = new DefaultBeanContainer();
        c2.getEnvironment().setProperty("feature.premium", "true");
        c2.scanComponents("com.minispring.samples.conditional");
        report(c2);

        System.out.println("\n=== 阶段7-2 示例结束 ===");
    }

    private static void report(DefaultBeanContainer container) {
        printBean(container, "basicService");
        printBean(container, "premiumService");
    }

    private static void printBean(DefaultBeanContainer container, String name) {
        try {
            container.getBean(name);
            System.out.println(name + ": 已注册");
        } catch (Exception e) {
            System.out.println(name + ": 未注册（条件不满足）");
        }
    }
}
```

- [ ] **Step 2: 编译并运行样例**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.conditional.ConditionalDemo"`
Expected: 输出依次包含
```
--- 场景1：未启用 premium（feature.premium 未设置）---
basicService: 已注册
premiumService: 未注册（条件不满足）

--- 场景2：启用 premium（feature.premium=true）---
basicService: 已注册
premiumService: 已注册
```

- [ ] **Step 3: 更新 README**

**(a) 特性概览表** —— 在 7-1 行之后追加 7-2 行。将：
```
| 7-1 | 事件机制 | `ApplicationEvent`/`ApplicationListener`、按类型路由的多播器、`@Autowired` 注入发布器、`ContextRefreshed`/`ContextClosed` 生命周期事件 |
```
改为（在其后新增一行）：
```
| 7-1 | 事件机制 | `ApplicationEvent`/`ApplicationListener`、按类型路由的多播器、`@Autowired` 注入发布器、`ContextRefreshed`/`ContextClosed` 生命周期事件 |
| 7-2 | 条件装配 | `@Conditional`/`Condition`、`ConditionEvaluator` 元注解解析、内置 `@ConditionalOnProperty` |
```

**(b) 项目结构 —— 顶层包区** —— 在 `event/` 块之后、`samples/` 之前插入 `condition/` 块。将：
```
│   │   └── ContextRefreshedEvent / ContextClosedEvent
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```
改为：
```
│   │   └── ContextRefreshedEvent / ContextClosedEvent
│   ├── condition/                # 条件装配（阶段 7-2）
│   │   ├── Conditional / Condition / ConditionContext
│   │   ├── ConditionEvaluator（元注解解析 + 求值）
│   │   └── ConditionalOnProperty / OnPropertyCondition
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```

**(c) 项目结构 —— samples 区** —— 在 `event/` 之后追加 `conditional/`。将：
```
│       └── event/                        # 阶段7-1 EventDemo（UserService / 事件 / 监听器）
```
改为：
```
│       ├── event/                        # 阶段7-1 EventDemo（UserService / 事件 / 监听器）
│       └── conditional/                  # 阶段7-2 ConditionalDemo（BasicService / PremiumService）
```

**(d) 运行示例命令** —— 在阶段7-1 命令之后追加。将：
```
# 阶段7-1 事件机制
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.event.EventDemo"
```
改为：
```
# 阶段7-1 事件机制
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.event.EventDemo"

# 阶段7-2 条件装配
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.conditional.ConditionalDemo"
```

**(e) 路线图表** —— 在 7-1 行之后插入 7-2 行。将：
```
| 7-1 | 事件机制（ApplicationEvent / Listener / 生命周期事件） | ✅ 已完成 |
| 7 | 高级特性其余部分（条件装配、国际化、异步、事务） | ⏳ 计划中 |
```
改为：
```
| 7-1 | 事件机制（ApplicationEvent / Listener / 生命周期事件） | ✅ 已完成 |
| 7-2 | 条件装配（@Conditional / @ConditionalOnProperty） | ✅ 已完成 |
| 7 | 高级特性其余部分（国际化、异步、事务） | ⏳ 计划中 |
```

- [ ] **Step 4: 运行全部测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/samples/conditional/BasicService.java \
        src/main/java/com/minispring/samples/conditional/PremiumService.java \
        src/main/java/com/minispring/samples/conditional/ConditionalDemo.java \
        README.md
git commit -m "feat(conditional): 新增阶段7-2 条件装配样例 ConditionalDemo 并更新 README" -- \
        src/main/java/com/minispring/samples/conditional/BasicService.java \
        src/main/java/com/minispring/samples/conditional/PremiumService.java \
        src/main/java/com/minispring/samples/conditional/ConditionalDemo.java \
        README.md
```

---

## 完成标准

- 全部 4 个任务提交，`mvn test` 全绿（含 Task 1-3 新增测试，无既有测试回归）。
- `ConditionalDemo` 可运行：未设属性时 `premiumService` 未注册、设 `feature.premium=true` 后已注册；`basicService` 两种场景都注册。
- 程序式 `registerBean(name, class)` 不评估条件（Task 3 测试覆盖）。
- 未引入任何新的第三方依赖；未改动 `pom.xml`、`ClassPathBeanScanner`、`registerBean`。
