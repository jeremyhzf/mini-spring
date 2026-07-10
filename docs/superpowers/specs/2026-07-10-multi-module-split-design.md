# 多模块拆分设计文档

> 将当前单 Maven 模块的 mini-spring 拆分为多模块结构，对齐最初设计文档的多模块意图，并适配阶段 7 的实际代码。
> 创建日期：2026-07-10。

## 1. 背景与目标

当前 mini-spring 是**单 Maven 模块**（`com.minispring:mini-spring:1.0.0-SNAPSHOT`，jar），核心容器、AOP、Web、阶段 7 五项高级特性、以及各阶段 Demo 全部混在一个 `src/` 里。最初设计文档（`docs/plans/2026-07-03-mini-spring-design.md`）本意是多模块（core/aop/web/tx/test/samples），实现时简化成了单模块。

### 目标

- 拆成多个 Maven 模块，**按依赖方向分层**，依赖 DAG 无环
- 让框架可作为**库被外部按需依赖**：核心容器可独立使用，Web/异步/事务等按需引入，samples 不污染发布物
- **保持 183 个测试全绿**，**源码兼容**（`com.minispring.*` 包名/类名不变，所有 `import com.minispring.*` 照常）

### 成功标准

- 拆分后 `mvn test`（aggregator）全绿，测试总数仍为 183
- 各模块可独立编译/测试；依赖方向符合 DAG，无循环
- 外部项目 `mvn install` 后按模块依赖（如 `mini-spring-core`）即可使用，`com.minispring.*` 导入不变
- samples 模块不被其他模块依赖（可单独不发布）

### 设计约束

- **不改 Java 包名**：保留 `com.minispring.*`（多模块共享基础包，模块边界≠包边界）
- 核心容器保持零第三方依赖（仅 JDK）；Web 用 provided Servlet API；测试用 JUnit 5
- 向后兼容：公开 API（包名/类名）不变，仅 Maven 坐标按模块拆分

## 2. 关键决策（brainstorming 已确认）

| 决策点 | 选择 |
|--------|------|
| 模块集合 | 6 模块：`mini-spring-aop` / `mini-spring-core` / `mini-spring-async` / `mini-spring-tx` / `mini-spring-web` / `mini-spring-samples` |
| event/condition/i18n 归属 | 并入 `mini-spring-core`（被 `DefaultBeanContainer` 直接引用，独立成模块会引入 core→子模块反向依赖或循环） |
| Java 包名 | 保留 `com.minispring.*` 不改（最小改动、最低风险，不动任何 import） |
| mini-spring-test | 不做（当前无实际测试基建，YAGNI） |
| 迁移策略 | 增量式逐模块抽取（按依赖序，每步验证保绿） |

## 3. 耦合探测结论（边界依据）

对 `DefaultBeanContainer`（factory，core 主体）的导入探测：

- **core 依赖**：`aop`（`Advisor`/`ProxyFactory`）、`event`、`condition`（`ConditionEvaluator`）、`i18n`（`MessageSource`）——这四者被容器直接引用。
- **core 不依赖**：`async`、`transaction`、`web`——`async`/`transaction` 是 `AroundAdvice`，经 `addAdvisor` 由用户接入，容器不导入它们；`web` 依赖容器而非反之。

由此决定：`aop` 为 base；`core`（含 event/condition/i18n）依 aop；`async`/`tx` 仅依 aop；`web` 依 core；samples 依全部。

## 4. 模块 → 包映射与依赖 DAG

| 模块 | 包含的包（`com.minispring.*`） | 编译依赖 | test 依赖 |
|------|-------------------------------|----------|-----------|
| `mini-spring-aop` | `aop` | 无 | JUnit |
| `mini-spring-core` | `factory` `annotation` `stereotype` `scanner` `env` `event` `condition` `i18n` | `mini-spring-aop` | JUnit |
| `mini-spring-async` | `async` | `mini-spring-aop` | JUnit + `mini-spring-core`（集成测试用容器） |
| `mini-spring-tx` | `transaction` | `mini-spring-aop` | JUnit + `mini-spring-core`（集成测试用容器） |
| `mini-spring-web` | `web` | `mini-spring-core` + `javax.servlet-api`(provided) | JUnit |
| `mini-spring-samples` | `samples` | `core` + `aop` + `async` + `tx` + `web` | — |

依赖 DAG（无环）：

```
        mini-spring-aop  (base, 仅 JDK)
       /        |        \
   core      async        tx
     |
   web
 (samples 依赖以上全部)
```

> 说明：core → aop（容器在 `applyAopProxy` 应用 AOP）。这是实现现实决定的单向依赖，DAG 无环。

## 5. pom 结构

### 5.1 根 parent pom（`mini-spring/pom.xml`）

- `<packaging>pom</packaging>`
- `<modules>`：aop、core、async、tx、web、samples
- 集中：`<properties>`（JDK 17、UTF-8、junit.version、servlet.version）、`<dependencyManagement>`（JUnit、servlet-api 的版本）、`<pluginManagement>`（compiler、exec、surefire）
- 不再含 `src/`（源码迁入各模块）

### 5.2 各模块 pom

- 统一 `groupId=com.minispring`、`version=1.0.0-SNAPSHOT`、`<parent>` 指向根
- 各 `artifactId=mini-spring-<name>`
- 按上表声明依赖（依赖经 parent 的 dependencyManagement 统一版本）
- `mini-spring-samples` 额外配置 `exec-maven-plugin`（运行各 `*Demo`）

## 6. 测试放置

- 各模块 `src/test/java` 随源码迁移：aop 测试→aop 模块、core 测试→core、async→async、tx→tx、web→web。
- **跨模块集成测试**：
  - `AsyncContainerIntegrationTest`（在 `async` 模块）用 core 的 `DefaultBeanContainer` → async 模块把 `mini-spring-core` 声明为 **test 作用域**依赖。
  - `TransactionalContainerIntegrationTest`（在 `tx` 模块）同理 → tx 模块 `mini-spring-core` 作 test 依赖。
  - core 模块内既有 AOP×容器集成测试（如 `AopContainerIntegrationTest`，位于 factory 测试包）→ 留在 core（core 编译依赖 aop，可用）。
- samples 模块无单元测试（仅可运行 Demo）。

## 7. 迁移策略（增量，每步验证）

按依赖序逐模块抽取，每步：`git mv` 迁源码与测试到模块 `src/` → 建模块 pom → 调整根 pom → `mvn test`（相关模块）验证绿 → 提交。

1. **根转 parent pom** + 建 6 模块目录骨架（空 pom/暂留 src 在根）。
2. **抽 `mini-spring-aop`**（base）：迁 `aop/` 源码+测试；aop 模块独立编译测试。
3. **抽 `mini-spring-core`**（→aop）：迁 factory/annotation/stereotype/scanner/env/event/condition/i18n 源码+测试；core 依赖 aop。
4. **抽 `mini-spring-async` 与 `mini-spring-tx`**（→aop，core 作 test 依赖）。
5. **抽 `mini-spring-web`**（→core + servlet provided）。
6. **抽 `mini-spring-samples`**（→全部），从根移除旧 `src/`。
7. **全量 `mvn test`**（aggregator）确认 183 全绿；更新 README/CLAUDE.md（结构、"如何作为库依赖"、运行命令按模块）。

> 机械性：全程 `git mv`（不改 `package` 声明/import）+ pom 调整；不改任何 `.java` 内容。每步 `mvn test` 保绿，可随时回退到上一提交。

## 8. 向后兼容与外部使用

- **源码兼容**：`com.minispring.*` 包名/类名全不变。现有 `import com.minispring.*` 代码只需把 Maven 依赖从 `com.minispring:mini-spring` 换成所需模块（如 `com.minispring:mini-spring-core`，会传递依赖 `mini-spring-aop`）。
- **按需依赖**：只用 IoC 容器 → `mini-spring-core`；要 MVC → 加 `mini-spring-web`；要异步/事务 → 加 `mini-spring-async`/`mini-spring-tx`。
- **可发布**：`mvn install` 各模块即可被本地其他项目依赖；要公开发布则 `mvn deploy`（Maven Central 需另行配置）。samples 可不发布。
- **Web 可选**：servlet-api 保持 `provided`，core 不强依赖 servlet 容器。

## 9. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 文件迁移量大（~180 文件）误放模块 | 严格按包→模块映射表；每步 `mvn test` 保绿即时发现错放 |
| 测试跨模块依赖漏配（async/tx 集成测试需 core） | 显式把 `mini-spring-core` 作 test 依赖（§6） |
| 依赖循环 | 已探测为无环 DAG（§3）；迁移按拓扑序保证 |
| `.git mv` 历史 / IDE 配置 | `.idea/` 已被跟踪，迁移后更新模块配置即可；git 历史经 `git mv` 保留 |
| 既有 `.java` 不应被改动 | 全程不改 `.java` 内容（仅移动 + pom），降低行为变更风险 |

## 10. 完成标准

- 6 模块结构成型，根为 parent pom，无残留旧 `src/`。
- `mvn test`（aggregator）全绿，测试总数 183 不变。
- 各模块可独立 `mvn test`；依赖方向符合 DAG，无循环。
- `com.minispring.*` 包名不变（验证：diff 中无 `.java` 内容改动，仅文件路径 + pom）。
- README/CLAUDE.md 更新多模块结构与"作为库依赖"说明。
