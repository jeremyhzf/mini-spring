# 国际化（MessageSource）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提供按 locale 解析消息码的 `MessageSource`（`ResourceBundleMessageSource` + `StaticMessageSource`），支持 `MessageFormat` 参数替换，容器可 `@Autowired` 注入。

**Architecture:** 新增 `com.minispring.i18n` 包；`AbstractMessageSource` 模板方法基类统一两个 `getMessage` 重载 + `MessageFormat` 参数替换，子类（`ResourceBundleMessageSource` / `StaticMessageSource`）只实现 `resolveCode`；`DefaultBeanContainer.resolveDependency` 增加 `MessageSource` 分支返回容器持有的实例。

**Tech Stack:** Java 17（`ResourceBundle` / `MessageFormat` / `Locale`，均为 JDK 自带）、JUnit 5、Maven。零新增第三方依赖。

## Global Constraints

- JDK 17+；`maven.compiler.source/target=17`。
- 核心容器**零第三方依赖**：本特性不得向 `pom.xml` 添加任何依赖（仅用 JDK 自带 `ResourceBundle`/`MessageFormat`/`Locale`）。
- 新增类型放 `com.minispring.i18n` 包；样例放 `com.minispring.samples.messagesource`；`.properties` 资源放 `src/main/resources` 或 `src/test/resources`（**UTF-8**，JDK 17 ResourceBundle 默认按 UTF-8 读取）。
- 全部代码注释、文档、提交信息使用中文；提交前缀 `feat(messagesource): ...` / `docs: ...`。
- TDD：每个任务先写失败测试，再实现，再验证通过，最后提交。
- **完全向后兼容**：仅给 `DefaultBeanContainer` 加字段/方法 + `resolveDependency` 加一个分支；不改现有行为。全量回归必须全绿。
- 容器通过反射实例化 Bean 时不对构造器调 setAccessible，故容器实例化的夹具/样例类必须 `public`（跨包可见）。
- 提交卫生：本仓库跟踪 `.idea/`，IDE 可能自动 stage `.idea/*`——提交时用**显式 pathspec**（`git commit -- <files>`），只提交本任务的文件。
- plain `mvn` works now（settings.xml jdk-1.8 profile 已关闭，JDK 17 source 生效）。

---

## 文件结构

**新增（main）：**
- `src/main/java/com/minispring/i18n/MessageSource.java` — 接口（2 个 getMessage 重载）
- `src/main/java/com/minispring/i18n/NoSuchMessageException.java` — RuntimeException
- `src/main/java/com/minispring/i18n/AbstractMessageSource.java` — 模板方法基类
- `src/main/java/com/minispring/i18n/StaticMessageSource.java` — 内存 Map 实现
- `src/main/java/com/minispring/i18n/ResourceBundleMessageSource.java` — .properties 实现
- `src/main/java/com/minispring/samples/messagesource/GreetingService.java`
- `src/main/java/com/minispring/samples/messagesource/MessageSourceDemo.java`
- `src/main/resources/messages.properties` / `messages_en.properties` / `messages_zh.properties`

**新增（test）：**
- `src/test/java/com/minispring/i18n/AbstractMessageSourceTest.java`
- `src/test/java/com/minispring/i18n/StaticMessageSourceTest.java`
- `src/test/java/com/minispring/i18n/ResourceBundleMessageSourceTest.java`
- `src/test/java/com/minispring/i18n/MessageSourceContainerIntegrationTest.java`
- `src/test/resources/i18n-test.properties` / `i18n-test_en.properties` / `i18n-test_zh.properties`

**修改：**
- `src/main/java/com/minispring/factory/DefaultBeanContainer.java` — 加字段/setter/`resolveDependency` 分支
- `README.md` — 结构树 / 运行命令 / 路线图 / 特性表

---

## Task 1: 消息源核心（MessageSource / NoSuchMessageException / AbstractMessageSource）

**Files:**
- Create: `src/main/java/com/minispring/i18n/MessageSource.java`
- Create: `src/main/java/com/minispring/i18n/NoSuchMessageException.java`
- Create: `src/main/java/com/minispring/i18n/AbstractMessageSource.java`
- Test: `src/test/java/com/minispring/i18n/AbstractMessageSourceTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `interface MessageSource { String getMessage(String code, Object[] args, Locale locale); String getMessage(String code, Object[] args, String defaultMessage, Locale locale); }`
  - `NoSuchMessageException extends RuntimeException`，构造 `(String code, Locale locale)`
  - `abstract class AbstractMessageSource implements MessageSource`，两个 `getMessage` 已实现；`protected abstract String resolveCode(String code, Locale locale)`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/i18n/AbstractMessageSourceTest.java`:

```java
package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractMessageSource 测试：参数替换、命中/未命中语义、默认值重载
 * 用一个测试专用的具体子类提供 resolveCode
 */
public class AbstractMessageSourceTest {

    private AbstractMessageSource source() {
        return new AbstractMessageSource() {
            @Override
            protected String resolveCode(String code, Locale locale) {
                switch (code) {
                    case "greeting": return "Hello,{0}";
                    case "plain": return "Hi";
                    default: return null;
                }
            }
        };
    }

    @Test
    void shouldResolveAndSubstituteArgs() {
        AbstractMessageSource src = source();
        assertEquals("Hello,Alice", src.getMessage("greeting", new Object[]{"Alice"}, Locale.ENGLISH));
    }

    @Test
    void shouldHandleNullArgsWhenNoPlaceholder() {
        AbstractMessageSource src = source();
        assertEquals("Hi", src.getMessage("plain", null, Locale.ENGLISH),
            "args==null 应当作空数组，无占位符时原样返回");
    }

    @Test
    void shouldThrowWhenCodeMissing() {
        AbstractMessageSource src = source();
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenCodeMissing() {
        AbstractMessageSource src = source();
        assertEquals("fallback", src.getMessage("missing", null, "fallback", Locale.ENGLISH));
    }

    @Test
    void defaultMessageOverloadShouldStillResolveWhenCodePresent() {
        AbstractMessageSource src = source();
        assertEquals("Hi", src.getMessage("plain", null, "fallback", Locale.ENGLISH),
            "代码存在时默认值重载应返回解析结果而非默认值");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=AbstractMessageSourceTest`
Expected: BUILD FAILURE —— 找不到符号 `MessageSource` / `NoSuchMessageException` / `AbstractMessageSource`

- [ ] **Step 3: 实现核心三件套**

`src/main/java/com/minispring/i18n/MessageSource.java`:

```java
package com.minispring.i18n;

import java.util.Locale;

/**
 * 国际化消息源接口
 * 按消息码 + locale 解析消息，支持 MessageFormat 参数替换（{0}、{1}…）
 */
public interface MessageSource {

    /**
     * 解析消息；找不到时抛 NoSuchMessageException
     *
     * @param code 消息码
     * @param args 占位符参数（可为 null）
     * @param locale 语言
     * @return 解析后的消息
     */
    String getMessage(String code, Object[] args, Locale locale);

    /**
     * 解析消息；找不到时返回 defaultMessage
     *
     * @param code           消息码
     * @param args           占位符参数（可为 null）
     * @param defaultMessage 找不到时的默认值
     * @param locale         语言
     * @return 解析后的消息，或 defaultMessage
     */
    String getMessage(String code, Object[] args, String defaultMessage, Locale locale);
}
```

`src/main/java/com/minispring/i18n/NoSuchMessageException.java`:

```java
package com.minispring.i18n;

import java.util.Locale;

/**
 * 找不到消息时抛出
 */
public class NoSuchMessageException extends RuntimeException {

    public NoSuchMessageException(String code, Locale locale) {
        super("No message found under code '" + code + "' for locale '" + locale + "'.");
    }
}
```

`src/main/java/com/minispring/i18n/AbstractMessageSource.java`:

```java
package com.minispring.i18n;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * 消息源模板方法基类
 * 统一两个 getMessage 重载与 MessageFormat 参数替换；
 * 子类只需实现 resolveCode 提供原始 pattern。
 */
public abstract class AbstractMessageSource implements MessageSource {

    @Override
    public String getMessage(String code, Object[] args, Locale locale) {
        String pattern = resolveCode(code, locale);
        if (pattern == null) {
            throw new NoSuchMessageException(code, locale);
        }
        return format(pattern, args, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String pattern = resolveCode(code, locale);
        if (pattern == null) {
            return defaultMessage;
        }
        return format(pattern, args, locale);
    }

    /**
     * 子类实现：按 code + locale 返回原始 pattern；不存在返回 null
     */
    protected abstract String resolveCode(String code, Locale locale);

    private String format(String pattern, Object[] args, Locale locale) {
        Object[] fmtArgs = (args == null) ? new Object[0] : args;
        return new MessageFormat(pattern, locale)
                .format(fmtArgs, new StringBuffer(), null)
                .toString();
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=AbstractMessageSourceTest`
Expected: BUILD SUCCESS，Tests run: 5

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/i18n/MessageSource.java \
        src/main/java/com/minispring/i18n/NoSuchMessageException.java \
        src/main/java/com/minispring/i18n/AbstractMessageSource.java \
        src/test/java/com/minispring/i18n/AbstractMessageSourceTest.java
git commit -m "feat(messagesource): 实现消息源核心 MessageSource/AbstractMessageSource" -- \
        src/main/java/com/minispring/i18n/MessageSource.java \
        src/main/java/com/minispring/i18n/NoSuchMessageException.java \
        src/main/java/com/minispring/i18n/AbstractMessageSource.java \
        src/test/java/com/minispring/i18n/AbstractMessageSourceTest.java
```

---

## Task 2: StaticMessageSource（内存 Map 实现）

**Files:**
- Create: `src/main/java/com/minispring/i18n/StaticMessageSource.java`
- Test: `src/test/java/com/minispring/i18n/StaticMessageSourceTest.java`

**Interfaces:**
- Consumes: `AbstractMessageSource`（Task 1）
- Produces: `StaticMessageSource`：`addMessage(String code, Locale locale, String message)`；继承两个 `getMessage`

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/i18n/StaticMessageSourceTest.java`:

```java
package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StaticMessageSource 测试：内存消息、locale 命中、参数替换、未命中语义
 */
public class StaticMessageSourceTest {

    @Test
    void shouldResolveAddedMessageWithArgs() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        assertEquals("你好,Alice", src.getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE));
    }

    @Test
    void shouldResolveByLocale() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.ENGLISH, "Hello,{0}");
        src.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        assertEquals("Hello,Bob", src.getMessage("greeting", new Object[]{"Bob"}, Locale.ENGLISH));
        assertEquals("你好,Bob", src.getMessage("greeting", new Object[]{"Bob"}, Locale.CHINESE));
    }

    @Test
    void shouldMissForUnknownLocaleOrCode() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.ENGLISH, "Hello");

        // 中文 locale 下没有消息
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("greeting", null, Locale.CHINESE));
        // 未知 code
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenMissing() {
        StaticMessageSource src = new StaticMessageSource();
        assertEquals("D", src.getMessage("missing", null, "D", Locale.ENGLISH));
    }

    @Test
    void shouldHandleNullArgsWhenNoPlaceholder() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("plain", Locale.ENGLISH, "Hi");
        assertEquals("Hi", src.getMessage("plain", null, Locale.ENGLISH));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=StaticMessageSourceTest`
Expected: BUILD FAILURE —— 找不到符号 `StaticMessageSource`

- [ ] **Step 3: 实现**

`src/main/java/com/minispring/i18n/StaticMessageSource.java`:

```java
package com.minispring.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 内存消息源：用 Map 存消息，便于测试与快速使用
 */
public class StaticMessageSource extends AbstractMessageSource {

    private final Map<Locale, Map<String, String>> messages = new HashMap<>();

    /**
     * 添加一条消息
     *
     * @param code    消息码
     * @param locale  语言
     * @param message 消息 pattern（可含 {0} 占位符）
     */
    public void addMessage(String code, Locale locale, String message) {
        messages.computeIfAbsent(locale, k -> new HashMap<>()).put(code, message);
    }

    @Override
    protected String resolveCode(String code, Locale locale) {
        Map<String, String> byCode = messages.get(locale);
        return (byCode == null) ? null : byCode.get(code);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=StaticMessageSourceTest`
Expected: BUILD SUCCESS，Tests run: 5

- [ ] **Step 5: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/i18n/StaticMessageSource.java \
        src/test/java/com/minispring/i18n/StaticMessageSourceTest.java
git commit -m "feat(messagesource): 实现 StaticMessageSource 内存消息源" -- \
        src/main/java/com/minispring/i18n/StaticMessageSource.java \
        src/test/java/com/minispring/i18n/StaticMessageSourceTest.java
```

---

## Task 3: ResourceBundleMessageSource（.properties 实现）

**Files:**
- Create: `src/main/java/com/minispring/i18n/ResourceBundleMessageSource.java`
- Create: `src/test/resources/i18n-test.properties`
- Create: `src/test/resources/i18n-test_en.properties`
- Create: `src/test/resources/i18n-test_zh.properties`
- Test: `src/test/java/com/minispring/i18n/ResourceBundleMessageSourceTest.java`

**Interfaces:**
- Consumes: `AbstractMessageSource`（Task 1）
- Produces: `ResourceBundleMessageSource`：`setBasename(String basename)`；继承两个 `getMessage`

- [ ] **Step 1: 写测试资源文件**

`src/test/resources/i18n-test.properties`（根，回退用）:

```properties
greeting=Hello,{0}
only.root=RootOnly
```

`src/test/resources/i18n-test_en.properties`:

```properties
greeting=Hi,{0}
```

`src/test/resources/i18n-test_zh.properties`（UTF-8，JDK 17 默认按 UTF-8 读取）:

```properties
greeting=你好,{0}
```

- [ ] **Step 2: 写失败测试**

`src/test/java/com/minispring/i18n/ResourceBundleMessageSourceTest.java`:

```java
package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceBundleMessageSource 测试：locale 命中、回退到根、参数替换、未命中、basename 缺失
 */
public class ResourceBundleMessageSourceTest {

    private ResourceBundleMessageSource source() {
        ResourceBundleMessageSource src = new ResourceBundleMessageSource();
        src.setBasename("i18n-test");
        return src;
    }

    @Test
    void shouldResolveEnglishBundle() {
        assertEquals("Hi,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.ENGLISH));
    }

    @Test
    void shouldResolveChineseBundle() {
        assertEquals("你好,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE));
    }

    @Test
    void shouldFallbackToRootBundle() {
        // 法语 bundle 不存在，回退到根 bundle
        assertEquals("Hello,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.FRENCH));
    }

    @Test
    void shouldResolveRootOnlyKeyWithFallback() {
        assertEquals("RootOnly", source().getMessage("only.root", null, Locale.ENGLISH));
    }

    @Test
    void shouldThrowWhenCodeMissing() {
        assertThrows(NoSuchMessageException.class,
            () -> source().getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenCodeMissing() {
        assertEquals("D", source().getMessage("missing", null, "D", Locale.ENGLISH));
    }

    @Test
    void shouldThrowWhenBasenameMissing() {
        ResourceBundleMessageSource src = new ResourceBundleMessageSource();
        src.setBasename("does-not-exist");
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("any", null, Locale.ENGLISH),
            "basename 完全不存在时应抛 NoSuchMessageException 而非崩溃");
    }
}
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `mvn test -Dtest=ResourceBundleMessageSourceTest`
Expected: BUILD FAILURE —— 找不到符号 `ResourceBundleMessageSource`

- [ ] **Step 4: 实现**

`src/main/java/com/minispring/i18n/ResourceBundleMessageSource.java`:

```java
package com.minispring.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 基于 classpath .properties 的消息源
 * basename 可配（如 "messages" 对应 messages.properties / messages_en.properties / messages_zh.properties）。
 * locale 回退复用 ResourceBundle.getBundle 原生策略（请求 locale → JVM 默认 → 根 bundle）。
 */
public class ResourceBundleMessageSource extends AbstractMessageSource {

    private String basename;

    /**
     * 设置资源 bundle 基名
     *
     * @param basename 基名（不含语言后缀与 .properties）
     */
    public void setBasename(String basename) {
        this.basename = basename;
    }

    @Override
    protected String resolveCode(String code, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(basename, locale);
            return bundle.containsKey(code) ? bundle.getString(code) : null;
        } catch (MissingResourceException e) {
            // basename 对应 bundle 完全不存在 → 视为找不到
            return null;
        }
    }
}
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `mvn test -Dtest=ResourceBundleMessageSourceTest`
Expected: BUILD SUCCESS，Tests run: 7

- [ ] **Step 6: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/i18n/ResourceBundleMessageSource.java \
        src/test/resources/i18n-test.properties \
        src/test/resources/i18n-test_en.properties \
        src/test/resources/i18n-test_zh.properties \
        src/test/java/com/minispring/i18n/ResourceBundleMessageSourceTest.java
git commit -m "feat(messagesource): 实现 ResourceBundleMessageSource 读取 classpath .properties" -- \
        src/main/java/com/minispring/i18n/ResourceBundleMessageSource.java \
        src/test/resources/i18n-test.properties \
        src/test/resources/i18n-test_en.properties \
        src/test/resources/i18n-test_zh.properties \
        src/test/java/com/minispring/i18n/ResourceBundleMessageSourceTest.java
```

---

## Task 4: 容器集成（@Autowired MessageSource 注入）

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`（加字段 + setter/getter + `resolveDependency` 分支）
- Test: `src/test/java/com/minispring/i18n/MessageSourceContainerIntegrationTest.java`

**Interfaces:**
- Consumes: `MessageSource`、`StaticMessageSource`（Task 1-2）；`DefaultBeanContainer.resolveDependency`（现有）
- Produces: `DefaultBeanContainer.setMessageSource(MessageSource)` / `getMessageSource()`；`@Autowired MessageSource` 注入容器持有的实例

- [ ] **Step 1: 写失败测试**

`src/test/java/com/minispring/i18n/MessageSourceContainerIntegrationTest.java`:

```java
package com.minispring.i18n;

import com.minispring.annotation.Autowired;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 容器注入 MessageSource 集成测试
 */
public class MessageSourceContainerIntegrationTest {

    /** 容器实例化的 Bean（跨包，须 public） */
    public static class Greeter {
        @Autowired
        private MessageSource messageSource;

        public String hello(String name, Locale locale) {
            return messageSource.getMessage("greeting", new Object[]{name}, locale);
        }
    }

    @Test
    void shouldInjectConfiguredMessageSource() {
        StaticMessageSource ms = new StaticMessageSource();
        ms.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.setMessageSource(ms);
        container.registerBean("greeter", Greeter.class);

        Greeter greeter = (Greeter) container.getBean("greeter");
        assertEquals("你好,Alice", greeter.hello("Alice", Locale.CHINESE),
            "@Autowired 注入的应为容器配置的 MessageSource");
    }

    @Test
    void shouldReturnNullWhenNotConfigured() {
        DefaultBeanContainer container = new DefaultBeanContainer();
        // 未 setMessageSource → getMessageSource 为 null（向后兼容，不自动创建）
        assertNull(container.getMessageSource());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn test -Dtest=MessageSourceContainerIntegrationTest`
Expected: BUILD FAILURE —— `DefaultBeanContainer` 无 `setMessageSource` 方法

- [ ] **Step 3: 修改 DefaultBeanContainer**

**(a) 新增字段** —— 在 `DefaultBeanContainer` 现有字段区（例如 `multicaster` 字段附近）加入：

```java
    // 国际化消息源
    private com.minispring.i18n.MessageSource messageSource;
```

**(b) 新增 setter/getter** —— 放在 `addAdvisor` 等公开方法附近：

```java
    /**
     * 设置国际化消息源（@Autowired MessageSource 注入此实例）
     *
     * @param messageSource 消息源
     */
    public void setMessageSource(com.minispring.i18n.MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 获取国际化消息源
     *
     * @return 消息源；未设置时为 null
     */
    public com.minispring.i18n.MessageSource getMessageSource() {
        return messageSource;
    }
```

**(c) resolveDependency 开头增加 MessageSource 分支** —— 将现有（`DefaultBeanContainer.java:518`）：

```java
    private Object resolveDependency(Class<?> type) {
        if (isInternalResolvableType(type)) {
            return this;
        }
        if (dependencyResolver == null) {
            dependencyResolver = new DependencyResolver(this);
        }
        return dependencyResolver.resolve(type);
    }
```

改为（在最前面加一个返回 `messageSource` 字段的分支，**不**并入 `isInternalResolvableType`，因为后者返回 `this`）：

```java
    private Object resolveDependency(Class<?> type) {
        if (type == com.minispring.i18n.MessageSource.class) {
            return messageSource;
        }
        if (isInternalResolvableType(type)) {
            return this;
        }
        if (dependencyResolver == null) {
            dependencyResolver = new DependencyResolver(this);
        }
        return dependencyResolver.resolve(type);
    }
```

不改动 `isInternalResolvableType`、`pom.xml`、其余任何方法。

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn test -Dtest=MessageSourceContainerIntegrationTest`
Expected: BUILD SUCCESS，Tests run: 2

- [ ] **Step 5: 运行全量测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过（既有容器/事件/条件测试不受影响）

- [ ] **Step 6: 提交（显式 pathspec）**

```bash
git add src/main/java/com/minispring/factory/DefaultBeanContainer.java \
        src/test/java/com/minispring/i18n/MessageSourceContainerIntegrationTest.java
git commit -m "feat(messagesource): DefaultBeanContainer 支持 @Autowired 注入 MessageSource" -- \
        src/main/java/com/minispring/factory/DefaultBeanContainer.java \
        src/test/java/com/minispring/i18n/MessageSourceContainerIntegrationTest.java
```

---

## Task 5: 样例 MessageSourceDemo + README 更新

**Files:**
- Create: `src/main/resources/messages.properties`
- Create: `src/main/resources/messages_en.properties`
- Create: `src/main/resources/messages_zh.properties`
- Create: `src/main/java/com/minispring/samples/messagesource/GreetingService.java`
- Create: `src/main/java/com/minispring/samples/messagesource/MessageSourceDemo.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `ResourceBundleMessageSource`、`@Autowired`、`DefaultBeanContainer.setMessageSource/scanComponents/getBean`（Task 1-4 + 现有）
- Produces: 可运行的 `com.minispring.samples.messagesource.MessageSourceDemo`

- [ ] **Step 1: 写资源文件**

`src/main/resources/messages.properties`（根）:

```properties
greeting=Hello,{0}
```

`src/main/resources/messages_en.properties`:

```properties
greeting=Hello,{0}
```

`src/main/resources/messages_zh.properties`（UTF-8）:

```properties
greeting=你好,{0}
```

- [ ] **Step 2: 写样例代码**

`src/main/java/com/minispring/samples/messagesource/GreetingService.java`:

```java
package com.minispring.samples.messagesource;

import com.minispring.annotation.Autowired;
import com.minispring.i18n.MessageSource;
import com.minispring.stereotype.Service;

import java.util.Locale;

/**
 * 问候服务：依赖注入 MessageSource，按 locale 取问候语
 */
@Service
public class GreetingService {

    @Autowired
    private MessageSource messageSource;

    public String greet(String name, Locale locale) {
        return messageSource.getMessage("greeting", new Object[]{name}, locale);
    }
}
```

`src/main/java/com/minispring/samples/messagesource/MessageSourceDemo.java`:

```java
package com.minispring.samples.messagesource;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.i18n.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * 阶段7-3 - 国际化示例
 * 演示：ResourceBundleMessageSource 读 .properties、多 locale、{0} 参数、@Autowired 注入
 */
public class MessageSourceDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-3：国际化 MessageSource 示例 ===\n");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.setMessageSource(messageSource);
        container.scanComponents("com.minispring.samples.messagesource");

        GreetingService service = (GreetingService) container.getBean("greetingService");

        System.out.println("English : " + service.greet("Alice", Locale.ENGLISH));
        System.out.println("中文     : " + service.greet("Alice", Locale.CHINESE));

        System.out.println("\n=== 阶段7-3 示例结束 ===");
    }
}
```

- [ ] **Step 3: 编译并运行样例**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.messagesource.MessageSourceDemo"`
Expected: 输出包含
```
English : Hello,Alice
中文     : 你好,Alice
```

- [ ] **Step 4: 更新 README**

**(a) 特性概览表** —— 在 7-2 行之后追加 7-3 行。将：
```
| 7-2 | 条件装配 | `@Conditional`/`Condition`、`ConditionEvaluator` 元注解解析、内置 `@ConditionalOnProperty` |
```
改为（在其后新增一行）：
```
| 7-2 | 条件装配 | `@Conditional`/`Condition`、`ConditionEvaluator` 元注解解析、内置 `@ConditionalOnProperty` |
| 7-3 | 国际化 | `MessageSource`、`ResourceBundleMessageSource`/`StaticMessageSource`、`MessageFormat` 参数替换、`@Autowired` 注入 |
```

**(b) 项目结构 —— 顶层包区** —— 在 `condition/` 块之后、`samples/` 之前插入 `i18n/` 块。将：
```
│   │   └── ConditionalOnProperty / OnPropertyCondition
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```
改为：
```
│   │   └── ConditionalOnProperty / OnPropertyCondition
│   ├── i18n/                     # 国际化（阶段 7-3）
│   │   ├── MessageSource / NoSuchMessageException / AbstractMessageSource
│   │   └── StaticMessageSource / ResourceBundleMessageSource
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
```

**(c) 项目结构 —— samples 区** —— 在 `conditional/` 之后追加 `messagesource/`。将：
```
│       └── conditional/                  # 阶段7-2 ConditionalDemo（BasicService / PremiumService）
```
改为：
```
│       ├── conditional/                  # 阶段7-2 ConditionalDemo（BasicService / PremiumService）
│       └── messagesource/                # 阶段7-3 MessageSourceDemo（GreetingService）
```

**(d) 运行示例命令** —— 在阶段7-2 命令之后追加。将：
```
# 阶段7-2 条件装配
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.conditional.ConditionalDemo"
```
改为：
```
# 阶段7-2 条件装配
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.conditional.ConditionalDemo"

# 阶段7-3 国际化
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.messagesource.MessageSourceDemo"
```

**(e) 路线图表** —— 在 7-2 行之后插入 7-3 行。将：
```
| 7-2 | 条件装配（@Conditional / @ConditionalOnProperty） | ✅ 已完成 |
| 7 | 国际化 `MessageSource` → 异步 `@Async` → 事务 `@Transactional` | ⏳ 计划中（按此顺序逐个推进） |
```
改为：
```
| 7-2 | 条件装配（@Conditional / @ConditionalOnProperty） | ✅ 已完成 |
| 7-3 | 国际化（MessageSource / 多 locale / 参数替换） | ✅ 已完成 |
| 7 | 异步 `@Async` → 事务 `@Transactional` | ⏳ 计划中（按此顺序逐个推进） |
```

- [ ] **Step 5: 运行全部测试，确认无回归**

Run: `mvn test`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 6: 提交（显式 pathspec）**

```bash
git add src/main/resources/messages.properties \
        src/main/resources/messages_en.properties \
        src/main/resources/messages_zh.properties \
        src/main/java/com/minispring/samples/messagesource/GreetingService.java \
        src/main/java/com/minispring/samples/messagesource/MessageSourceDemo.java \
        README.md
git commit -m "feat(messagesource): 新增阶段7-3 国际化样例 MessageSourceDemo 并更新 README" -- \
        src/main/resources/messages.properties \
        src/main/resources/messages_en.properties \
        src/main/resources/messages_zh.properties \
        src/main/java/com/minispring/samples/messagesource/GreetingService.java \
        src/main/java/com/minispring/samples/messagesource/MessageSourceDemo.java \
        README.md
```

---

## 完成标准

- 全部 5 个任务提交，`mvn test` 全绿（含 Task 1-4 新增测试，无既有测试回归）。
- `MessageSourceDemo` 可运行：English 输出 `Hello,Alice`、中文输出 `你好,Alice`。
- `@Autowired MessageSource` 注入的是容器配置的实例；未配置时 `getMessageSource()` 返回 null（向后兼容）。
- 未引入任何新的第三方依赖；未改动 `pom.xml`、`isInternalResolvableType`、其余现有方法。
