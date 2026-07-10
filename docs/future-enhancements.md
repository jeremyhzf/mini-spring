# 后期增强与拓展方向

> 阶段 7 全部五项高级特性（事件、条件装配、国际化、异步、事务）已完成。本文梳理后续可选的增强与拓展方向，按"各特性 YAGNI 待办"与"跨特性拓展"两类组织，并给出优先级建议。
> 创建日期：2026-07-10。每一项都可沿用既有节奏独立走 **brainstorming（spec）→ writing-plans（plan）→ subagent-driven / TDD 实现**。

## 1. 各特性的 YAGNI 增强项

下列各项在对应特性的 spec「范围边界」中明确标注为"不做"，是各自最自然的下一步。

### 1.1 事件机制（7-1）

- **`@EventListener` 注解式**：方法级注解，任意 Bean 的任意方法都能监听（无需实现 `ApplicationListener` 接口）。需新增一个 `BeanPostProcessor` 扫描 `@EventListener` 方法、按参数类型注册。对标 Spring 的 `EventListenerMethodProcessor`。
- **真正的异步事件分发**：多播器已预留 `Executor` 扩展点（同步为默认），接入线程池即可；与 7-4 `@Async` 的 `Executor` 可共享。
- **`PayloadApplicationEvent` / `publishEvent(Object)`**：允许发布任意对象（自动包装为 payload 事件），对标 Spring。
- **`@Order` / `SmartListener`**：监听器排序；当前 `CopyOnWriteArraySet` 无序。
- **`@TransactionalEventListener`**：事务相位事件（BEFORE_COMMIT/AFTER_COMMIT 等），需事务同步回调支撑。

### 1.2 条件装配（7-2）

- **`@ConditionalOnClass` / `@ConditionalOnBean`**：classpath 类存在 / 容器中 Bean 存在才注册。需 `ConditionContext` 暴露 classloader 与 bean 注册表（当前只暴露 `Environment` + 候选类）。
- **`@Profile`**：基于激活 profile 的条件，可由 `@Conditional` 组合实现。
- **`@Bean` 方法级条件**：等 `@Configuration`/`@Bean` 支持后（见 §2.2），条件装配可作用于 `@Bean` 方法。
- **`ConditionContext` 扩展**：暴露 `BeanDefinitionRegistry` / `ResourceLoader` / `ClassLoader`。

### 1.3 国际化（7-3）

- **`MessageSourceResolvable` 第三重载**：批量解析。
- **`LocaleResolver` / 容器默认 Locale**：当前 `getMessage` 显式传 `Locale`；可加一个 locale 解析器（如基于线程/请求）。
- **`MessageSourceAware` 回调**：Bean 实现 `MessageSourceAware` 自动注入消息源（当前靠 `@Autowired`）。
- **`DelegatingMessageSource` 自动默认**：未配置时返回 code 本身，而非 null。
- **嵌套消息解析**：消息值内引用另一 code（如 `msg={another}`）。
- **`setFallbackToSystemLocale` 控制**：当前直接用 `ResourceBundle` 默认回退。

### 1.4 异步（7-4）

- **具体类 `@Async`**：当前受零依赖所限（无真实 CGLIB）只能作用于接口方法。引入 CGLIB（见 §2.1）即可解除。
- **`@Async("executorName")`**：按名称选择具名 `Executor`（需容器管理多个 executor bean）。
- **`AsyncUncaughtExceptionHandler`**：自定义 void 异步方法的未捕获异常处理（当前 `printStackTrace`）。
- **`Future` / `ListenableFuture` 返回**：当前仅 `void` + `CompletableFuture`。
- **类级 `@Async`**：标注在类上则所有方法异步。
- **`@EnableAsync` 自动装配**：当前靠 `addAdvisor` 手动接入；可做后处理器自动注册。

### 1.5 事务（7-5）

- **真实 DB 事务**：实现一个对接 JDBC 的 `DataSourceTransactionManager`（`Connection.setAutoCommit(false)/commit()/rollback()` + `ThreadLocal` 绑定 Connection）。`SimpleTransactionManager` 是逻辑事务、可观测但不持久。
- **其余 5 种传播**：`SUPPORTS` / `NOT_SUPPORTED` / `NEVER` / `MANDATORY` / `NESTED`（当前仅 REQUIRED / REQUIRES_NEW）。
- **隔离级别 / 超时 / 只读**：`@Transactional(isolation/timeout/readOnly)`。
- **`TransactionSynchronizationManager` 完整回调**：`beforeCommit`/`afterCommit`/`afterCompletion` 等，支撑 `@TransactionalEventListener`。
- **`@EnableTransactionManagement` 自动装配**：当前靠 `addAdvisor` 手动接入。

## 2. 跨特性拓展

这些方向超出单个特性的范围，会同时影响多个子系统或容器内核。

### 2.1 真实 CGLIB（解除接口约束）

- **现状**：`CglibProxy` 实为 JDK 代理（核心零依赖，无 cglib 库），导致 `@Async`/`@Transactional` 只能作用于接口方法。
- **方向**：引入 cglib（或字节码生成），让具体类也可被代理。需权衡：破坏"核心容器零依赖"原则。可选方案——把 AOP 代理做成**可选模块**（核心保持 JDK 代理，引入 cglib 依赖后启用 CGLIB），或仅在 Web 层 / 独立模块引入。
- **收益**：`@Async`/`@Transactional` 可标注在具体类方法上，更贴近日常 Spring 用法。

### 2.2 `@Configuration` / `@Bean` 注解驱动配置

- **现状**：Bean 注册只有组件扫描（`@Component` 系）与程序式 `registerBean`，无 `@Bean` 方法。
- **方向**：`@Configuration` 类 + `@Bean` 方法，让 Bean 定义可编程。这是 `@Conditional`/`@Async`/`@Transactional` 作用于方法级的前提（条件装配、事务、异步都能在 `@Bean` 方法上生效）。
- **关联**：解锁 §1.2 的方法级条件、`@Profile` 等。

### 2.3 `ApplicationContext` 抽象

- **现状**：`DefaultBeanContainer` 同时扮演容器、事件发布器、消息源持有者、条件评估入口等多重角色。
- **方向**：抽出 `ApplicationContext` 接口（聚合容器 + Environment + MessageSource + 事件 + 资源），对标 Spring。`DefaultBeanContainer` 退化为 `BeanFactory`，`ApplicationContext` 在其上加事件/i18n/生命周期。
- **收益**：职责分离，更接近 Spring 的分层。

### 2.4 Environment 与属性源

- **现状**：`StandardEnvironment` 用内存 Map 存属性；`@Value` 占位符 `${...}` 经它解析。
- **方向**：多 `PropertySource`（系统属性、环境变量、`.properties`/`.yaml` 文件、命令行参数）、`@Profile` 激活、`PropertyResolver` 统一抽象。
- **关联**：`@ConditionalOnProperty`、`@Profile` 都依赖更强的 Environment。

### 2.5 SpEL 表达式

- **现状**：`@Value` 只做 `${...}` 占位符替换（字面值）。
- **方向**：`@Value("#{...}")` SpEL 表达式（支持方法调用、运算、bean 引用）。零依赖下需自实现一个最小表达式求值器（或作为可选依赖）。

### 2.6 Web 层增强

- **现状**：`DispatcherServlet` + `HandlerMapping` + `@RequestMapping` + `ViewResolver`。
- **方向**：`@RequestBody`/`@ResponseBody`（JSON 序列化）、`@PathVariable`、`HandlerExceptionResolver`、`@ControllerAdvice`、参数解析器与返回值处理器体系、REST 风格。

### 2.7 AOP 增强

- **现状**：JDK 动态代理 + AroundAdvice/BeforeAdvice/AfterAdvice + Advisor/Pointcut + 责任链。
- **方向**：`@Aspect`/`@Pointcut`/`@Before`/`@After`/`@Around` 注解式切面（对标 `@AspectJ`），从注解自动构建 Advisor。

## 3. 优先级建议

按"投入产出比 + 与既有架构契合度"粗略分档：

**第一档（自然下一步，复用既有机制，改动可控）**
- `@EventListener` 注解式（事件）—— 一个后处理器即可，高教学价值。
- `@ConditionalOnProperty` 之外的 `@ConditionalOnClass`/`@ConditionalOnBean`（条件）—— 扩展 `ConditionContext` 即可。
- 真正的异步事件分发（事件）—— 多播器 `Executor` 扩展点已预留。
- `LocaleResolver` / 容器默认 Locale（i18n）—— 小而常用。

**第二档（中等投入，需内核扩展）**
- `@Configuration`/`@Bean`（§2.2）—— 解锁方法级条件/事务/异步，影响面较大但价值高。
- 其余事务传播 + 隔离级别（事务）—— `SimpleTransactionManager` 已有 ThreadLocal 框架。
- `ApplicationContext` 抽象（§2.3）—— 职责重构，触及 `DefaultBeanContainer`。

**第三档（较大投入或权衡依赖原则）**
- 真实 DB 事务（事务）—— 需引入 JDBC 场景 / 可选模块。
- 真实 CGLIB（§2.1）—— 权衡零依赖原则，建议作为可选模块。
- SpEL（§2.5）—— 自实现表达式求值器成本高。
- Web `@RequestBody`/`@ResponseBody`（§2.6）—— 需 JSON 序列化（引入或自实现）。

## 4. 工作流

每一项增强都建议沿用阶段 7 沉淀的节奏（spec/plan 可作模板，见 `docs/superpowers/specs|plans/`）：

1. **brainstorming**：明确目的/约束/成功标准，2-3 方案择优，分节定稿设计。
2. **writing-plans**：拆成 TDD bite-sized 任务，每步含完整代码与测试。
3. **subagent-driven / TDD 实现**：每任务独立 subagent + 两阶段 review（spec 合规 + 质量），末尾整分支 review。
4. 文档同步：README / CLAUDE.md / 对应 phase 文档。

> 提醒（已写入 CLAUDE.md）：阶段 7-5 期间发现并修复的 AOP 缺陷（代理 `proceed()` 未解包 `InvocationTargetException`，commit `82cd35b`）——任何新增的"异常感知型 advice"都已因此受益；后续若再遇 advice 收到包装异常的现象，可先确认该修复仍在。
