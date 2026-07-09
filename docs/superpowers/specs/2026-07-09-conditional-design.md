# 阶段 7-2：条件装配（@Conditional）设计文档

> Mini-Spring 阶段 7「高级特性」的第二个子系统。本特性独立 spec → plan → 实现。
> 创建日期：2026-07-09

## 1. 背景与目标

阶段 7-1 完成了事件机制。本 spec 实现条件装配：组件扫描时，根据运行时条件决定是否把某个候选类注册为 Bean。

### 目标

- 提供 `@Conditional` 注解与 `Condition` 接口，让用户用自定义条件类控制 Bean 是否注册
- 提供内置便捷注解 `@ConditionalOnProperty`（复用已有 `Environment`），覆盖最常见的"按属性开关 Bean"场景
- 通过**元注解解析**让 `@ConditionalOnProperty` 自身被 `@Conditional` 标注——对应 Spring 的注解元模型机制

### 成功标准

- 标了 `@Conditional(Cond.class)` 的组件：`Cond.matches` 返回 `true` 时注册、`false` 时跳过
- 标了 `@ConditionalOnProperty(name, havingValue, matchIfMissing)` 的组件：`Environment` 属性匹配时注册，否则跳过；属性缺失时按 `matchIfMissing` 决定
- 经任意层组合注解间接标注的 `@Conditional` 能被正确解析（元注解链）
- 无 `@Conditional` 的组件注册行为完全不变（向后兼容）
- 零新增第三方依赖；附完整单元测试、集成测试与可运行样例

### 设计约束

- 核心容器保持零外部依赖
- 遵循扁平顶层包风格与渐进式、TDD 驱动的项目惯例
- 条件评估只在**注解扫描路径**（`scanComponents`）生效；程序式 `registerBean(name, class)` 不评估条件（对标 Spring 手动 register 不走条件）

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| 作用范围 | 仅类级别（组件类）。mini-spring 无 `@Configuration`/`@Bean`，注册只走 `scanComponents → registerBean` 一条路径 |
| 功能面 | 核心 + `@ConditionalOnProperty`（不做 `@ConditionalOnClass`/`@ConditionalOnBean`/`@Profile`） |
| 整体架构 | 方案 1：新增 `com.minispring.condition` 包，独立 `ConditionEvaluator`；`DefaultBeanContainer.scanComponents` 委托它判断 |
| ConditionContext 暴露面 | `Environment` + 候选类 `Class<?>`（不暴露 registry/classloader） |
| 条件生效路径 | 仅 `scanComponents`；`registerBean(name, class)` 程序式注册不评估条件 |

## 3. 组件清单

新增顶层包 **`com.minispring.condition`**。所有类型均为新增（对 `DefaultBeanContainer` 的改动见第 5 节）。

| 类型 | 角色 | 职责 |
|------|------|------|
| `@Conditional` | 注解 | `Class<? extends Condition> value()`；`@Target(TYPE) @Retention(RUNTIME)` |
| `Condition` | 接口 | `boolean matches(ConditionContext context)` |
| `ConditionContext` | 上下文 | `Environment getEnvironment()` + `Class<?> getCandidate()`（被评估的候选类） |
| `ConditionEvaluator` | 求值器 | 元注解解析 + 实例化 Condition + 求值；`boolean shouldRegister(Class<?> candidate)`；构造时传入 `Environment` |
| `@ConditionalOnProperty` | 内置便捷注解 | 自身标注 `@Conditional(OnPropertyCondition.class)`；属性 `name / havingValue / matchIfMissing` |
| `OnPropertyCondition` | 内置 Condition | 从候选类读 `@ConditionalOnProperty` 属性，查 `Environment.getProperty(name)` 比对 |

## 4. 数据流

```
scanComponents(basePackage)
  → ClassPathBeanScanner.scan() 得候选类集合 Set<Class<?>>
  → ConditionEvaluator evaluator = new ConditionEvaluator(getEnvironment())
  → for each candidate:
      if (evaluator.shouldRegister(candidate))
          registerBean(generateBeanName(candidate), candidate)
      // 否则跳过，不注册
```

`ConditionEvaluator.shouldRegister(candidate)` 内部：

```
1. 递归遍历候选类注解链，收集所有 @Conditional 的 Condition 类
   （直接标注 @Conditional，或经 @ConditionalOnProperty 等间接标注，都算）
2. 收集为空 → 返回 true（正常注册）
3. 否则：对每个 Condition 类
     - 反射实例化（无参构造）
     - cond.matches(new ConditionContext(environment, candidate))
   全部为 true 才返回 true（AND 语义）
```

## 5. 集成点与现有代码改动

唯一修改的现有类是 `DefaultBeanContainer`，仅改 `scanComponents` 方法：

当前实现（节选）：
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

改为在注册前用 `ConditionEvaluator` 判断：
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

不修改：`pom.xml`、`ClassPathBeanScanner`、`registerBean`、`Environment`、其余任何现有类。

## 6. 元注解解析（教学亮点）

候选类 `C` 标了 `@ConditionalOnProperty`，而 `@ConditionalOnProperty` 的定义上又标了 `@Conditional(OnPropertyCondition.class)`。`ConditionEvaluator` 必须透过这层间接关系找到 `@Conditional`——这正是 Spring `AnnotatedElementUtils.findMergedAnnotation` 解决的问题。

收集算法（递归，带已访问集合防环）：

```
visit(AnnotatedElement element, 收集 result, 已访问 seen):
    for (Annotation a : element.getAnnotations()):
        type = a.annotationType()
        if (type 已在 seen) continue;  seen.add(type)
        if (type == Conditional.class):
            result.add(((Conditional) a).value())     // 直接标注 @Conditional
        else:
            c = type.getDeclaredAnnotation(Conditional.class)
            if (c != null) result.add(c.value())       // 经组合注解间接标注
            visit(type, result, seen)                  // 继续向上找更深层的元注解
```

入口 `findConditions(candidate)`：对候选类调用 `visit`，返回收集到的 Condition 类集合（`LinkedHashSet` 保序去重）。

## 7. 错误处理

- **Condition 抛异常**或**Condition 类无法实例化**（无无参构造等）→ 异常向外传播，注册流程失败（不静默吞，便于排错）
- **无 `@Conditional` 的候选** → `shouldRegister` 返回 `true`，正常注册（向后兼容）
- **`@ConditionalOnProperty.name` 对应属性缺失** → `matchIfMissing == true` 时注册，否则跳过
- **属性值与 `havingValue` 比对**：`havingValue` 为空字符串时表示"只要属性存在即匹配"（属性值非 null）

## 8. 测试策略

### 单元测试

- `ConditionEvaluatorTest`
  - 无 `@Conditional` 候选 → `shouldRegister` 为 `true`
  - `@Conditional(TrueCondition.class)` → `true`；`@Conditional(FalseCondition.class)` → `false`
  - AND 语义：候选类经组合注解携带 `TrueCondition`，又直接标 `@Conditional(FalseCondition)` → 两个可达条件中有一个为 false → `shouldRegister` 为 `false`
  - 元注解解析：候选标自定义组合注解 `@MyMarker`（其上标 `@Conditional(TrueCondition.class)`）→ 被识别为 `true`
- `OnPropertyConditionTest`
  - `name` 属性 == `havingValue` → 匹配
  - 不等 → 不匹配
  - 属性缺失 + `matchIfMissing=false` → 不匹配；`matchIfMissing=true` → 匹配
  - `havingValue` 为空 + 属性存在 → 匹配
- `ConditionContextTest`
  - `getEnvironment()` / `getCandidate()` 返回构造时传入的对象

### 集成测试

- `ConditionalContainerIntegrationTest`
  - 设环境属性 → `scanComponents` 后条件 Bean 可 `getBean` 取到
  - 不设属性 → `getBean` 抛 `BeanNotFoundException`
  - 同包内无注解组件仍正常注册（回归）
  - 程序式 `registerBean(name, class)` 即便类上有 `@Conditional` 仍注册（确认条件只在扫描路径生效）

### 样例

`com.minispring.samples.conditional.ConditionalDemo`：一个常驻 `@Service` + 一个 `@ConditionalOnProperty` 的 `@Service`；先不设属性跑一次（只剩常驻），再 `setProperty` 跑一次（两者都在）。同步更新 README（项目结构 / 运行命令 / 路线图 阶段 7-2）。

## 9. 范围边界（明确不做）

- `@ConditionalOnClass` / `@ConditionalOnBean` / `@Profile`（需暴露 classloader / registry）
- `@Bean` 方法级条件（mini-spring 无 `@Configuration`/`@Bean`）
- `ConditionContext` 暴露 registry / classloader / resourceLoader
- `AnnotatedTypeMetadata`（Spring 传给 `matches` 的第二参数；mini-spring 直接把候选 `Class<?>` 放进 `ConditionContext`）
- 多 Condition 的 OR 语义（只做 AND）

## 10. 与真实 Spring 的对应关系（教学要点）

| Mini-Spring | Spring Framework |
|-------------|------------------|
| `@Conditional` | `org.springframework.context.annotation.Conditional` |
| `Condition` | `org.springframework.context.annotation.Condition` |
| `ConditionContext` | 同名（精简版：仅 Environment + 候选类） |
| `ConditionEvaluator` | 同名（Spring 内部求值器） |
| `@ConditionalOnProperty` | `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`（属 Spring Boot） |
| 元注解解析 `findConditions` | `AnnotatedElementUtils.findMergedAnnotation` |
