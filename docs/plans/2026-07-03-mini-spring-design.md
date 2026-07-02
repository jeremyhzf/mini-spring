# Mini-Spring 框架设计文档

## 概述

Mini-Spring 是一个从零开发的轻量级Spring框架教学项目，采用渐进式构建方法，每个阶段都是一个可运行的最小实现。本项目面向高级开发者，旨在深入理解Spring框架的核心设计思想、架构模式和最佳实践。

## 技术栈

- **JDK 17+**：利用现代Java特性（record、sealed类、模式匹配）
- **无外部依赖**：核心功能完全自实现
- **JUnit 5**：用于单元测试
- **Maven**：项目构建和依赖管理

## 项目结构

```
mini-spring/
├── mini-spring-core/          # 核心容器（阶段1-4）
├── mini-spring-aop/           # AOP实现（阶段5）
├── mini-spring-web/           # Web框架（阶段6）
├── mini-spring-tx/            # 事务管理（阶段7）
├── mini-spring-test/          # 测试支持
└── mini-spring-samples/       # 示例应用
```

## 演进路径

### 阶段1 - 简单Bean容器（2-3天）

**目标**：实现最基础的Bean容器，理解依赖注入的起点

**核心类**：
```java
public interface BeanContainer {
    void registerBean(String name, Class<?> clazz);
    Object getBean(String name);
}
```

**功能**：
- 通过反射实例化对象
- Map存储Bean实例
- 按名称获取Bean
- 异常处理（未找到、实例化失败）

**教学重点**：反射基础、容器概念、异常处理

---

### 阶段2 - 依赖注入支持（3-4天）

**目标**：实现Bean之间的依赖关系注入

**核心组件**：
- `ConstructorResolver`：解析构造器参数
- `DependencyResolver`：递归解析依赖关系
- 支持构造器注入和Setter注入
- 循环依赖检测

**设计模式**：Builder模式、Strategy模式

**教学重点**：依赖解析算法、设计原则（SOLID）

---

### 阶段3 - Bean生命周期与作用域（3-4天）

**目标**：实现完整的Bean生命周期管理

**新增接口**：
- `BeanPostProcessor`：前后置处理
- `InitializingBean` / `DisposableBean`：生命周期回调
- `Scope`：Singleton、Prototype作用域实现

**设计模式**：责任链模式处理多个后处理器

**教学重点**：生命周期管理、单例缓存

---

### 阶段4 - 注解驱动开发（4-5天）

**目标**：支持现代Spring的注解编程模型

**核心组件**：
- `ClassPathBeanScanner`：扫描classpath
- `AnnotationBeanDefinitionReader`：解析注解元数据
- `AutowiredAnnotationProcessor`：处理自动装配

**支持的注解**：
- `@Component` / `@Repository` / `@Service` / `@Controller`
- `@Autowired` / `@Qualifier`
- `@Value` / `@Scope`

**教学重点**：注解处理、类加载机制、元注解设计

---

### 阶段5 - AOP实现（5-7天）

**目标**：实现面向切面编程支持

**核心设计**：
- `ProxyFactory`：统一代理创建入口
- `JdkDynamicProxy`：基于接口的JDK代理
- `CglibProxy`：基于继承的CGLIB代理
- `Advice`接口体系：Before/After/AfterReturning/AfterThrowing/Around
- `Advisor`：切点+增强组合
- `MethodMatcher`：切点匹配逻辑

**教学重点**：动态代理、责任链、反射修改行为

---

### 阶段6 - MVC框架（7-10天）

**目标**：构建完整的Web层框架

**核心组件**：
- `DispatcherServlet`：前端控制器
- `HandlerMapping`：请求路径映射
- `HandlerAdapter`：统一处理器调用
- `HandlerArgumentResolver`：参数解析
- `HandlerReturnValueHandler`：返回值处理
- `ViewResolver`：视图解析
- `ExceptionResolver`：异常处理

**请求处理流程**：
1. 请求到达 → DispatcherServlet
2. HandlerMapping查找处理器
3. HandlerAdapter执行处理器
4. 参数解析 → 方法调用 → 返回值处理
5. 视图渲染/响应写入

**教学重点**：Servlet规范、责任链模式、策略模式

---

### 阶段7 - 高级特性（5-7天）

**目标**：实现企业级应用所需的高级特性

**特性列表**：
- 事务管理：`@Transactional`注解，基于AOP
- 异步处理：`@Async`注解，线程池集成
- 条件装配：`@Conditional`注解
- 事件机制：ApplicationEvent/Listener
- 国际化：MessageSource

**教学重点**：高级特性与核心容器的集成

---

## 学习建议

**前置知识**：
- 熟悉Java基础和反射
- 理解面向对象设计原则
- 了解常见设计模式
- 基础Servlet/Web知识（阶段6需要）

**每个阶段包含**：
1. 核心概念讲解（设计原理）
2. 代码实现（带详细注释）
3. 单元测试（验证功能）
4. 示例应用（展示用法）
5. 与真实Spring对比（理解差异）

**学习成果**：
完成所有阶段后，将深入理解Spring框架的核心设计思想，能够阅读Spring源码，并在实际工作中更好地使用和扩展Spring框架。

## 版本历史

- 2026-07-03：初始设计完成
