# 阶段 7-3：国际化（MessageSource）设计文档

> Mini-Spring 阶段 7「高级特性」的第三个子系统。本特性独立 spec → plan → 实现。
> 创建日期：2026-07-09

## 1. 背景与目标

阶段 7-1（事件机制）、7-2（条件装配）已完成。本 spec 实现国际化：按 locale 解析消息码，支持参数占位符替换。

### 目标

- 提供 `MessageSource` 接口，按 `(code, args, locale)` 解析消息，参数用 `MessageFormat`（`{0}` 占位符）替换
- 提供 `ResourceBundleMessageSource`（读 classpath `.properties`）与 `StaticMessageSource`（内存 Map，便于测试）
- 容器持有 `MessageSource`，Bean 可 `@Autowired` 注入（复用 events 阶段建的 `resolveDependency` 钩子）

### 成功标准

- `getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE)` 能从 `messages_zh.properties` 取到 `你好,{0}` 并替换为 `你好,Alice`
- 切换 locale（如 `Locale.ENGLISH`）取到对应语言的消息；locale 缺失时回退到根 bundle
- 代码不存在时：无默认值重载抛 `NoSuchMessageException`；带默认值重载返回默认值
- `@Autowired MessageSource` 注入的是容器配置的实例
- 零新增第三方依赖（仅用 JDK `ResourceBundle` / `MessageFormat` / `Locale`）；附完整单元测试、集成测试与可运行样例

### 设计约束

- 核心容器保持零外部依赖
- 遵循扁平顶层包风格与渐进式、TDD 驱动的项目惯例
- 向后兼容：不改任何现有行为

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| 集成深度 | 容器集成：接口 + ResourceBundleMessageSource + StaticMessageSource；`@Autowired` 可注入 |
| API 重载 | 2 个：`getMessage(code, args, locale)` 抛异常；`getMessage(code, args, defaultMessage, locale)` 返回默认。不做 `MessageSourceResolvable` |
| Locale 模型 | 显式 `Locale` 参数（调用方决定），不做 LocaleResolver / 容器默认 Locale |
| 回退策略 | 复用 `ResourceBundle.getBundle(basename, locale)` 原生 locale 回退（请求 locale → JVM 默认 → 根 bundle） |
| 模式 | `AbstractMessageSource` 模板方法基类统一参数替换与两个重载，子类只实现 `resolveCode` |
| 默认实例 | 用户 `setMessageSource(ms)` 显式配置；未配置注入 null（不做 DelegatingMessageSource 自动默认） |

## 3. 组件清单

新增顶层包 **`com.minispring.i18n`**。所有类型均为新增（对 `DefaultBeanContainer` 的改动见第 5 节）。

| 类型 | 角色 | 职责 |
|------|------|------|
| `MessageSource` | 接口 | `getMessage(String code, Object[] args, Locale locale)`（找不到抛 `NoSuchMessageException`）；`getMessage(String code, Object[] args, String defaultMessage, Locale locale)`（找不到返回 `defaultMessage`） |
| `NoSuchMessageException` | 异常 | `RuntimeException`，找不到消息时抛 |
| `AbstractMessageSource` | 模板方法基类 | `implements MessageSource`；实现两个 `getMessage`：调用子类 `resolveCode` 取 pattern，用 `MessageFormat`（按 locale）替换 args，pattern 为 null 时按重载语义抛异常或返回默认；`protected abstract String resolveCode(String code, Locale locale)` |
| `ResourceBundleMessageSource` | 文件实现 | `extends AbstractMessageSource`；`setBasename(String)`；`resolveCode` 用 `ResourceBundle.getBundle(basename, locale)`，捕获 `MissingResourceException` 返回 null |
| `StaticMessageSource` | 内存实现 | `extends AbstractMessageSource`；`addMessage(String code, Locale locale, String msg)`；内部 `Map<Locale, Map<String,String>>`；`resolveCode` 查表 |

## 4. 数据流

```
bean.@Autowired MessageSource
  → DefaultBeanContainer.resolveDependency(MessageSource.class) 返回容器的 messageSource 字段
  → bean 调用 messageSource.getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE)
  → AbstractMessageSource.getMessage:
      pattern = resolveCode("greeting", Locale.CHINESE)   // 子类查表/读 bundle
      若 pattern == null → 抛 NoSuchMessageException（或返回默认）
      否则 new MessageFormat(pattern, locale).format(args, StringBuffer, null).toString()
  → "你好,Alice"
```

`ResourceBundleMessageSource.resolveCode`：
```
try {
    ResourceBundle bundle = ResourceBundle.getBundle(basename, locale);
    return bundle.containsKey(code) ? bundle.getString(code) : null;
} catch (MissingResourceException e) {
    return null;   // basename 对应 bundle 完全不存在
}
```

`StaticMessageSource.resolveCode`：
```
Map<String,String> byCode = messages.get(locale);
return byCode == null ? null : byCode.get(code);
```

## 5. 集成点与现有代码改动

唯一修改的现有类是 `DefaultBeanContainer`。当前 `resolveDependency`（节选，`src/main/java/com/minispring/factory/DefaultBeanContainer.java:518-526`）：

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

改动：

**(a) 新增字段（放在 multicaster / environment 字段附近）：**

```java
// 国际化消息源
private com.minispring.i18n.MessageSource messageSource;
```

**(b) 新增 setter/getter：**

```java
/**
 * 设置国际化消息源（@Autowired MessageSource 注入此实例）
 */
public void setMessageSource(com.minispring.i18n.MessageSource messageSource) {
    this.messageSource = messageSource;
}

/**
 * 获取国际化消息源
 */
public com.minispring.i18n.MessageSource getMessageSource() {
    return messageSource;
}
```

**(c) resolveDependency 开头增加 MessageSource 分支（返回字段，非 this）：**

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

> 注意：与 `ApplicationEventPublisher`（返回容器本身 `this`）不同，`MessageSource` 是容器持有的独立实例，故单独分支返回 `messageSource` 字段，不并入 `isInternalResolvableType`。

不修改：`pom.xml`、`isInternalResolvableType`、`Environment`、其余任何现有方法。

## 6. 错误处理

- **pattern 为 null**（代码不存在或 bundle 缺失）：`getMessage(code, args, locale)` 抛 `NoSuchMessageException`；`getMessage(code, args, defaultMessage, locale)` 返回 `defaultMessage`
- **basename 对应 bundle 完全不存在**：`ResourceBundle.getBundle` 抛 `MissingResourceException` → `resolveCode` 捕获返回 null → 按上条处理（不向上传播崩溃）
- **`args == null`**：`AbstractMessageSource` 当作空数组处理（`MessageFormat.format(pattern)` 无可替换参数）
- **`MessageFormat` 解析失败**（pattern 语法错误）：按 JDK 行为抛出（不额外包装）

## 7. 测试策略

### 单元测试

- `StaticMessageSourceTest`：
  - `addMessage` + `getMessage` 命中（含 `{0}` 参数替换）
  - locale 命中：`getMessage("hi", args, Locale.CHINESE)` 返回中文
  - 未命中抛 `NoSuchMessageException`
  - 默认值重载返回默认值
  - `args == null` 不报错
- `ResourceBundleMessageSourceTest`（测试专属 basename，避免与样例冲突）：
  - `src/test/resources` 下放 `i18n-test.properties`（根）、`i18n-test_en.properties`、`i18n-test_zh.properties`
  - locale 命中：`getMessage` 按 `Locale.ENGLISH`/`Locale.CHINESE` 取到对应语言
  - 回退：请求某 locale 的 bundle 不存在时回退到根 bundle
  - 参数替换（`{0}`）
  - 代码不存在抛 `NoSuchMessageException` + 默认值重载
  - basename 完全不存在（设为不存在的 basename）→ 抛 `NoSuchMessageException`（不崩溃）

### 集成测试

- `MessageSourceContainerIntegrationTest`：
  - `container.setMessageSource(staticMs)`，`@Autowired MessageSource` 注入的实例即所设；调用 `getMessage` 正确返回
  - （向后兼容）不设置时既有行为不变

### 样例

`com.minispring.samples.messagesource.MessageSourceDemo`：用 `ResourceBundleMessageSource`（basename `messages`），`src/main/resources` 下放 `messages.properties` / `messages_en.properties` / `messages_zh.properties`；演示同一 code 在 `Locale.ENGLISH` 与 `Locale.CHINESE` 下返回不同语言 + `{0}` 参数。同步更新 README（项目结构 / 运行命令 / 路线图 阶段 7-3）。

## 8. 范围边界（明确不做）

- `MessageSourceResolvable` 第三重载
- `LocaleResolver` / 容器默认 Locale / 当前线程 Locale
- `MessageSourceAware` 回调接口
- `DelegatingMessageSource` 自动默认实例
- 嵌套消息解析（消息值内引用另一 code）
- `setFallbackToSystemLocale` 控制（直接用 ResourceBundle 默认回退）
- bundle 缓存控制（ResourceBundle 自带缓存）

## 9. 与真实 Spring 的对应关系（教学要点）

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `MessageSource` | `org.springframework.context.MessageSource` |
| `NoSuchMessageException` | 同名，`org.springframework.context` |
| `AbstractMessageSource` | 同名（模板方法 + MessageFormat） |
| `ResourceBundleMessageSource` | 同名，`org.springframework.context.support` |
| `StaticMessageSource` | 同名 |
| `resolveDependency` 注入 MessageSource | ApplicationContext 自身即 MessageSource + `@Autowired` |
