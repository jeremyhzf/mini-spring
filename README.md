# Mini-Spring

> 从零开发的轻量级 Spring 框架教学项目 —— 用渐进式、TDD 驱动的方式，亲手实现 Spring 的核心机制。

Mini-Spring 是一个面向高级开发者的手写 Spring 教学框架。它不追求功能完备，而是**从第一行反射代码开始**，逐阶段重建 IoC 容器、依赖注入、生命周期、注解驱动、AOP 与 MVC —— 让你真正理解 Spring 在背后做了什么。

核心容器**零外部依赖**，全部用 Java 反射与标准库自实现；Web 层仅依赖 Servlet API。

## ✨ 特性概览

按阶段循序渐进（阶段 1-6 为核心阶段，阶段 7 为高级特性），每阶段都是一个可独立运行、带完整单元测试的最小实现：

| 阶段 | 主题 | 核心能力 |
|------|------|---------|
| 1 | Bean 容器 | 反射实例化、Map 存储、按名获取、单例缓存 |
| 2 | 依赖注入 | 构造器注入、Setter 注入、按类型解析、循环依赖检测 |
| 3 | 生命周期与作用域 | `BeanPostProcessor`、初始化/销毁回调、Singleton/Prototype 作用域 |
| 4 | 注解驱动 | `@Component`/`@Service`/`@Repository`/`@Controller`、`@Autowired`/`@Qualifier`/`@Value`、组件扫描 |
| 5 | AOP | JDK 动态代理、CGLIB 代理、Advice 体系、Advisor/Pointcut、责任链拦截器 |
| 6 | MVC | `DispatcherServlet` 前端控制器、`HandlerMapping`、`@RequestMapping` 系列注解、`ViewResolver` |
| 7-1 | 事件机制 | `ApplicationEvent`/`ApplicationListener`、按类型路由的多播器、`@Autowired` 注入发布器、`ContextRefreshed`/`ContextClosed` 生命周期事件 |
| 7-2 | 条件装配 | `@Conditional`/`Condition`、`ConditionEvaluator` 元注解解析、内置 `@ConditionalOnProperty` |
| 7-3 | 国际化 | `MessageSource`、`ResourceBundleMessageSource`/`StaticMessageSource`、`MessageFormat` 参数替换、`@Autowired` 注入 |
| 7-4 | 异步 | `@Async`（接口方法）、`AsyncInterceptor`（`AroundAdvice`）、`Executor` 异步执行、`void`/`CompletableFuture` 返回 |

> 阶段 7 的事件机制、条件装配、国际化与异步已实现；其余高级特性（事务）见 [路线图](#-路线图)，尚未实现。

## 🛠 技术栈

- **JDK 17+**（使用现代 Java 特性）
- **Maven** —— 构建与依赖管理
- **JUnit 5** —— 单元测试
- **Servlet API 4.0**（`javax.servlet-api`，`provided` 作用域，仅 Web 层）
- 核心容器：**零第三方依赖**

## 📦 项目结构

单 Maven 模块，按功能域分包：

```
mini-spring/
├── src/main/java/com/minispring/
│   ├── factory/                  # IoC 容器（阶段 1-3）
│   │   ├── BeanContainer.java            # 容器顶层接口
│   │   ├── DefaultBeanContainer.java     # 容器默认实现（贯穿各阶段的核心）
│   │   ├── BeanNotFoundException.java
│   │   ├── instantiator/                 # 实例化：ConstructorResolver / SetterInjector
│   │   ├── dependency/                   # 依赖解析：DependencyResolver / CircularDependencyDetector
│   │   ├── lifecycle/                    # 生命周期：InitializingBean / DisposableBean / BeanPostProcessor
│   │   └── scope/                        # 作用域：Scope / SingletonScope / PrototypeScope / ScopeRegistry
│   ├── annotation/               # @Autowired / @Qualifier / @Value（阶段 4）
│   ├── stereotype/               # @Component / @Service / @Repository / @Controller（阶段 4）
│   ├── scanner/                  # ClassPathBeanScanner 组件扫描（阶段 4）
│   ├── env/                      # Environment / StandardEnvironment（@Value 占位符解析）
│   ├── aop/                      # AOP（阶段 5）
│   │   ├── Advisor / Pointcut / MethodMatcher / ClassFilter / DefaultAdvisor
│   │   ├── advice/                       # Advice / BeforeAdvice / AfterAdvice / AroundAdvice / MethodInvocation
│   │   ├── proxy/                        # ProxyFactory / JdkDynamicProxy / CglibProxy
│   │   └── interceptor/                  # LoggingInterceptor / TransactionInterceptor / PerformanceMonitorInterceptor
│   ├── web/                      # MVC（阶段 6）
│   │   ├── ModelAndView.java
│   │   ├── annotation/                   # @RequestMapping / @GetMapping / @PostMapping / @RequestParam
│   │   ├── servlet/                      # DispatcherServlet / HandlerMapping / HandlerExecutionChain / HandlerInterceptor
│   │   └── view/                         # View / ViewResolver / InternalResourceView / InternalResourceViewResolver
│   ├── event/                    # 事件机制（阶段 7-1）
│   │   ├── ApplicationEvent / ApplicationListener / ApplicationEventPublisher
│   │   ├── ApplicationEventMulticaster / SimpleApplicationEventMulticaster / ErrorHandler
│   │   ├── GenericTypeResolver / ApplicationListenerDetector
│   │   └── ContextRefreshedEvent / ContextClosedEvent
│   ├── condition/                # 条件装配（阶段 7-2）
│   │   ├── Conditional / Condition / ConditionContext
│   │   ├── ConditionEvaluator（元注解解析 + 求值）
│   │   └── ConditionalOnProperty / OnPropertyCondition
│   ├── i18n/                     # 国际化（阶段 7-3）
│   │   ├── MessageSource / NoSuchMessageException / AbstractMessageSource
│   │   └── StaticMessageSource / ResourceBundleMessageSource
│   ├── async/                    # 异步（阶段 7-4）
│   │   ├── Async（注解）
│   │   └── AsyncInterceptor（AroundAdvice，提交 Executor）
│   └── samples/                  # 各阶段示例（按阶段分包，每个含一个 *Demo 入口）
│       ├── ioc/                          # 阶段1 BeanContainerDemo
│       ├── di/                           # 阶段2 DependencyInjectionDemo
│       ├── lifecycle/                    # 阶段3 LifecycleDemo
│       ├── annotation/                   # 阶段4 AnnotationDemo（UserService / UserRepository）
│       ├── aop/                          # 阶段5 AopDemo（OrderService / IOrderService）
│       ├── mvc/                          # 阶段6 MvcDemo（UserController）
│       ├── event/                        # 阶段7-1 EventDemo（UserService / 事件 / 监听器）
│       ├── conditional/                  # 阶段7-2 ConditionalDemo（BasicService / PremiumService）
│       ├── messagesource/                # 阶段7-3 MessageSourceDemo（GreetingService）
│       └── async/                        # 阶段7-4 AsyncDemo（NotificationService）
├── src/test/java/com/minispring/         # 各阶段单元测试 + 集成测试
└── docs/
    ├── plans/                            # 各阶段实施计划
    └── phases/                           # 各阶段完成检查清单
```

## 🚀 快速开始

### 环境要求

- JDK 17 或更高
- Maven 3.6+

### 构建与测试

```bash
# 编译
mvn clean compile

# 运行全部单元测试
mvn test
```

### 运行示例应用

每阶段一个独立可运行的 Demo 入口，集中位于 `com.minispring.samples` 包下：

```bash
# 阶段1 Bean 容器
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.ioc.BeanContainerDemo"

# 阶段2 依赖注入
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.di.DependencyInjectionDemo"

# 阶段3 生命周期与作用域
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.lifecycle.LifecycleDemo"

# 阶段4 注解驱动
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.annotation.AnnotationDemo"

# 阶段5 AOP
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.aop.AopDemo"

# 阶段6 MVC
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.mvc.MvcDemo"

# 阶段7-1 事件机制
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.event.EventDemo"

# 阶段7-2 条件装配
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.conditional.ConditionalDemo"

# 阶段7-3 国际化
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.messagesource.MessageSourceDemo"

# 阶段7-4 异步
mvn compile exec:java -Dexec.mainClass="com.minispring.samples.async.AsyncDemo"
```

## 📖 核心用法

### 创建容器、注册与获取 Bean（阶段 1）

```java
BeanContainer container = new DefaultBeanContainer();
container.registerBean("userService", UserService.class);
UserService service = (UserService) container.getBean("userService");
```

### 注解驱动 + 组件扫描 + AOP（阶段 4 & 5）

```java
DefaultBeanContainer container = new DefaultBeanContainer();

// 配置 AOP：对 create 开头的方法织入事务，对所有方法织入日志
container.addAdvisor(new DefaultAdvisor(
        new TransactionInterceptor(),
        (method, targetClass) -> method.getName().startsWith("create")));
container.addAdvisor(new DefaultAdvisor(
        new LoggingInterceptor(),
        (method, targetClass) -> true));

// 扫描包下带 @Service / @Component 等注解的类，自动注册为 Bean
int count = container.scanComponents("com.minispring.samples.aop");

// 获取的 Bean 已被 AOP 代理增强
IOrderService orderService = (IOrderService) container.getBean("orderService");
orderService.createOrder("ORD-001");  // 触发事务 + 日志
orderService.cancelOrder("ORD-001");  // 仅触发日志
```

### MVC 控制器（阶段 6）

```java
@RequestMapping("/user")
public class UserController {

    @GetMapping("/list")
    public ModelAndView listUsers() {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user/list");
        mav.addObject("users", /* ... */);
        return mav;
    }

    @PostMapping("/create")
    public String createUser(@RequestParam("name") String name) {
        return "User created: " + name;
    }
}
```

```java
DispatcherServlet servlet = new DispatcherServlet();
servlet.registerController(new UserController());
```

### 事件机制：发布-订阅（阶段 7-1）

自定义事件、按类型路由的监听器、依赖注入的发布器：

```java
// 自定义事件
public class UserCreatedEvent extends ApplicationEvent {
    private final String name;
    public UserCreatedEvent(Object source, String name) { super(source); this.name = name; }
    public String getName() { return name; }
}

// 监听器：实现 ApplicationListener<E>，只接收类型匹配的事件
@Component
public class UserCreatedListener implements ApplicationListener<UserCreatedEvent> {
    @Override
    public void onApplicationEvent(UserCreatedEvent event) {
        System.out.println("收到用户创建事件: " + event.getName());
    }
}

// 发布方：注入发布器并发布
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void register(String name) {
        publisher.publishEvent(new UserCreatedEvent(this, name));
    }
}
```

```java
DefaultBeanContainer container = new DefaultBeanContainer();
container.scanComponents("com.minispring.samples.event");
container.refresh();   // eager 实例化单例，广播 ContextRefreshedEvent

UserService service = (UserService) container.getBean("userService");
service.register("Alice");   // 监听器收到 UserCreatedEvent

container.destroy();   // 广播 ContextClosedEvent（先于单例销毁）
```

> 各阶段的设计原理、教学重点与代码注释详见 [`docs/`](docs/) 下的实施计划与完成检查清单。

## 🧪 测试

测试代码与各阶段一一对应，覆盖从 Bean 容器基础到 AOP/MVC/事件机制的全部功能与边界场景：

```bash
mvn test        # 运行全部测试
```

关键测试类包括 `BeanContainerTest`、`DependencyResolverTest`、`LifecycleTest`、`AnnotationDrivenTest`、`ProxyFactoryTest`、`DispatcherServletTest`、`SimpleApplicationEventMulticasterTest`、`GenericTypeResolverTest`、`EventContainerPublishTest`、`EventContainerLifecycleTest` 等。

## 🗺 路线图

| 阶段 | 主题 | 状态 |
|------|------|------|
| 1 | 简单 Bean 容器 | ✅ 已完成 |
| 2 | 依赖注入支持 | ✅ 已完成 |
| 3 | Bean 生命周期与作用域 | ✅ 已完成 |
| 4 | 注解驱动开发 | ✅ 已完成 |
| 5 | AOP 实现 | ✅ 已完成 |
| 6 | MVC 框架 | ✅ 已完成 |
| 7-1 | 事件机制（ApplicationEvent / Listener / 生命周期事件） | ✅ 已完成 |
| 7-2 | 条件装配（@Conditional / @ConditionalOnProperty） | ✅ 已完成 |
| 7-3 | 国际化（MessageSource / 多 locale / 参数替换） | ✅ 已完成 |
| 7-4 | 异步（@Async / AsyncInterceptor） | ✅ 已完成 |
| 7 | 事务 `@Transactional` | ⏳ 计划中 |

## 🎯 学习目标

完成本项目后，你将能够：

- 深入理解 IoC / DI 的反射与递归解析实现
- 掌握 Bean 生命周期、作用域与后处理器（`BeanPostProcessor`）的责任链设计
- 看透注解驱动编程模型背后的类加载与元数据处理
- 理解 JDK 动态代理与 CGLIB 代理在 AOP 中的应用
- 重建 Servlet 基础上的前端控制器 MVC 架构
- 理解发布-订阅事件机制与容器生命周期事件的设计
- 进而更自如地阅读 Spring 源码，并在工程中更好地使用与扩展 Spring

## 📄 许可

本项目为学习用途，随附源码自由使用。
