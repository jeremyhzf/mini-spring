# 阶段 7-1：事件机制（ApplicationEvent / ApplicationListener）设计文档

> Mini-Spring 阶段 7「高级特性」的第一个子系统。本特性独立 spec → plan → 实现，
> 后续特性（条件装配、国际化、异步、事务）各自单独成文。
>
> 创建日期：2026-07-09

## 1. 背景与目标

阶段 1–6 已完成 IoC 容器、依赖注入、生命周期、注解驱动、AOP、MVC。阶段 7 引入企业级应用所需的高级特性，本 spec 只覆盖**事件机制**。

### 目标

在核心容器上构建一套 Spring 风格的发布-订阅事件机制，使任意 Bean 能够：

1. 发布自定义事件
2. 实现接口注册为监听器，按事件类型被动接收
3. 响应容器自身的生命周期事件（启动就绪 / 关闭）

### 成功标准

- 任意 Bean 可通过被注入的 `ApplicationEventPublisher` 发布自定义事件，类型匹配的监听器收到
- 实现 `ApplicationListener<E>` 的 Bean 被容器自动发现并注册，无需手动登记
- 容器就绪时广播 `ContextRefreshedEvent`，关闭时广播 `ContextClosedEvent`
- 多播器内部预留 `Executor` 扩展点（默认同步），为后续 `@Async` 阶段铺路
- 零新增第三方依赖；附完整单元测试、集成测试与可运行样例

### 设计约束

- 核心容器保持零外部依赖（仅复用现有 JUnit 5 测试 + Servlet API provided，本特性不引入任何新依赖）
- 遵循现有扁平顶层包风格与渐进式、TDD 驱动、每阶段可独立运行的项目惯例
- 完全向后兼容：不调用新增的 `refresh()` 时，容器行为与今天一致（懒加载）

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| API 风格 | 接口式：`ApplicationEvent` + `ApplicationListener<E>` + 多播器按类型路由 |
| 生命周期事件 | 包含：容器就绪 / 关闭时自动广播 |
| 同步 / 异步 | 默认同步；多播器内部预留 `Executor` 扩展点 |
| 整体架构 | 方案 1：以 `ApplicationEventMulticaster` 为核心；容器委托发布；后处理器自动收集监听器 |
| 懒加载与生命周期事件的调和 | 方案 A：新增 opt-in `refresh()`，eager 实例化单例后广播就绪事件；不调则保持懒加载 |

## 3. 组件清单

新增顶层包 **`com.minispring.event`**。所有类型均为新增（对 `DefaultBeanContainer` 的改动见第 5 节）。

| 类型 | 角色 | 职责 |
|------|------|------|
| `ApplicationEvent` | 事件基类 | 继承 `java.util.EventObject`，持有 `source`（通常为容器本身） |
| `ApplicationListener<E extends ApplicationEvent>` | 监听器接口 | `void onApplicationEvent(E event)`；由泛型 `E` 决定关心的事件类型 |
| `ApplicationEventPublisher` | 发布器接口 | `void publishEvent(ApplicationEvent event)` |
| `ApplicationEventMulticaster` | 事件引擎接口 | `addApplicationListener` / `removeApplicationListener` / `multicastEvent(event)` |
| `SimpleApplicationEventMulticaster` | 默认实现 | 持有监听器集合，按事件运行时类型路由分发；内含可选 `Executor`（同步/异步）与 `ErrorHandler` 扩展点 |
| `ApplicationListenerDetector` | `BeanPostProcessor` | 在 `postProcessAfterInitialization` 时，若 Bean 实现 `ApplicationListener` 就注册进多播器 |
| `GenericTypeResolver` | 泛型解析工具 | 从 `ApplicationListener<E>` 提取 `E`，决定监听器关心的事件类型（对应 Spring 的 `ResolvableType`） |
| `ContextRefreshedEvent` | 生命周期事件 | 容器就绪时广播 |
| `ContextClosedEvent` | 生命周期事件 | 容器关闭时广播 |

> 包名取舍：采用扁平风格的 `com.minispring.event`。若后续希望原样贴近 Spring，可整体迁移到 `com.minispring.context.event`，本次不阻塞。

## 4. 数据流

### 4.1 监听器注册流

```
refresh()
  → 快照遍历所有已注册 bean 定义
  → 对 singleton 作用域逐个 getBean(name)（eager 实例化；prototype 跳过）
  → 每个实例化经过 applyPostProcessAfterInitialization
  → ApplicationListenerDetector 检测到 ApplicationListener → 注册进多播器
```

### 4.2 发布 → 分发流

```
bean 调用 publisher.publishEvent(myEvent)
  → DefaultBeanContainer.publishEvent 委托给 multicaster.multicastEvent(event)
  → 多播器遍历监听器：GenericTypeResolver 解析每个监听器的 E
  → 若 E.isAssignableFrom(event 的运行时类型) → 调用 listener.onApplicationEvent(event)
  → Executor == null：当前线程同步执行
  → Executor != null：提交 Runnable 到 executor（异步扩展点）
```

### 4.3 生命周期事件流

```
refresh() 末尾 → publishEvent(new ContextRefreshedEvent(container))
destroy() 开头 → publishEvent(new ContextClosedEvent(container))  // 先广播，随后才销毁单例
```

## 5. 集成点与现有代码改动

唯一需要修改的现有类是 `DefaultBeanContainer`：

- `implements ApplicationEventPublisher`
- 新增字段 `ApplicationEventMulticaster multicaster`，构造时创建 `SimpleApplicationEventMulticaster`
- 构造器内部自动注册一个 `ApplicationListenerDetector`（持有 multicaster 引用）为后处理器——用户无需手动注册
- 新增 `publishEvent(ApplicationEvent event)` → 委托 `multicaster.multicastEvent(event)`
- 新增 `refresh()`：快照遍历 `beanDefinitions`，对 singleton 作用域逐个 `getBean`；结束后 `publishEvent(new ContextRefreshedEvent(this))`
- `destroy()`：在原有销毁单例逻辑**之前**插入 `publishEvent(new ContextClosedEvent(this))`，保证监听器此时仍存活

不修改：`pom.xml`（零新依赖）、`ClassPathBeanScanner`、其余任何现有类。

## 6. 错误处理

- **默认**：监听器抛异常从 `multicastEvent` 向外传播，**中断**后续监听器（对齐 Spring `SimpleApplicationEventMulticaster` 默认行为）
- **扩展点**：多播器可设 `ErrorHandler`；设置后异常交其处理并**继续**其余监听器
- **关闭健壮性**：`ContextClosedEvent` 的广播包在 `try/catch` 内，避免某个监听器抛错导致单例无法销毁；`ContextRefreshedEvent` 保持默认传播
- **裸类型退化**：`GenericTypeResolver` 无法解析泛型时（裸 `ApplicationListener` 无类型参数）→ 退化为接收**所有**事件（对齐 Spring 裸类型语义）

## 7. 测试策略

### 单元测试

- `SimpleApplicationEventMulticasterTest`
  - 按类型路由：发布事件 A，仅 A 监听器与"全事件"监听器触发
  - 监听器增删
  - 异步分支：用"捕获型 Executor"收集提交的 Runnable，断言它们未被同步执行
  - 默认异常传播；设置 `ErrorHandler` 后继续执行剩余监听器
- `GenericTypeResolverTest`
  - 具体子类（如 `class MyListener implements ApplicationListener<UserCreatedEvent>`）
  - 匿名监听器
  - 裸监听器（解析失败 → 退化为 `ApplicationEvent`，接收全部）

### 集成测试

- `EventContainerIntegrationTest`
  - 注册自定义事件 + 监听器 Bean，`publishEvent` 后断言监听器收到
  - 生命周期：`refresh()` 后 `ContextRefreshedEvent` 监听器触发
  - 关闭顺序：`destroy()` 时 `ContextClosedEvent` 触发，且发生在单例销毁**之前**（用一个 `DisposableBean` 置标志位，监听器检查 Bean 仍存活来验证顺序）

### 样例

`com.minispring.samples.event.EventDemo`：自定义事件（如 `UserCreatedEvent`）+ 监听器 + 演示生命周期事件。同步更新 README 的项目结构、运行命令与路线图（阶段 7-1 标记完成）。

## 8. 范围边界（明确不做，留作后续）

- `@EventListener` 注解式（方法级，后续增强）
- 真正的异步执行（本次仅预留 `Executor` 槽；实际异步在 `@Async` 阶段）
- `PayloadApplicationEvent` / `publishEvent(Object)` 载荷重载
- `@Order` / `SmartListener` 监听器排序
- `@TransactionalEventListener` 事务绑定事件

## 9. 与真实 Spring 的对应关系（教学要点）

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `ApplicationEvent` | `org.springframework.context.ApplicationEvent` |
| `ApplicationListener<E>` | `org.springframework.context.ApplicationListener` |
| `ApplicationEventPublisher` | `org.springframework.context.ApplicationEventPublisher` |
| `ApplicationEventMulticaster` / `SimpleApplicationEventMulticaster` | 同名，`org.springframework.context.event` |
| `ApplicationListenerDetector` | 同名（Spring 内部后处理器） |
| `GenericTypeResolver` | `ResolvableType` |
| `ContextRefreshedEvent` / `ContextClosedEvent` | 同名 |
