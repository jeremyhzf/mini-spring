# 阶段4 - 注解驱动开发实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现代码注解驱动开发，支持@Component、@Autowired、@Qualifier、@Value等注解，实现组件扫描功能

**架构:** 创建注解定义、注解处理器、组件扫描器，扩展现有容器支持注解驱动的Bean注册和装配

**技术栈:** Java 17, JUnit 5, Maven（无外部依赖）

---

## 前置准备

### Task 0: 创建注解相关包结构

**Files:**
- Create: `src/main/java/com/minispring/stereotype/`
- Create: `src/main/java/com/minispring/annotation/`
- Create: `src/main/java/com/minispring/scanner/`
- Create: `src/test/java/com/minispring/stereotype/`
- Create: `src/test/java/com/minispring/annotation/`
- Create: `src/test/java/com/minispring/scanner/`

**Step 1: 创建目录结构**

Run: `mkdir -p src/main/java/com/minispring/stereotype src/main/java/com/minispring/annotation src/main/java/com/minispring/scanner src/test/java/com/minispring/stereotype src/test/java/com/minispring/annotation src/test/java/com/minispring/scanner`

**Step 2: 提交目录结构**

```bash
git add .
git commit -m "feat: 创建注解相关包结构"
```

---

## 组件注解定义

### Task 1: 定义组件注解

**Files:**
- Create: `src/main/java/com/minispring/stereotype/Component.java`
- Create: `src/main/java/com/minispring/stereotype/Repository.java`
- Create: `src/main/java/com/minispring/stereotype/Service.java`
- Create: `src/main/java/com/minispring/stereotype/Controller.java`
- Test: `src/test/java/com/minispring/stereotype/ComponentTest.java`

**Step 1: 编写测试 - 定义注解行为**

```java
package com.minispring.stereotype;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ComponentTest {

    @Test
    void shouldDefineComponentAnnotation() {
        assertNotNull(TestComponent.class.getAnnotation(Component.class));
    }

    @Test
    void shouldDefineRepositoryAnnotation() {
        assertNotNull(TestRepository.class.getAnnotation(Repository.class));
    }

    @Test
    void shouldDefineServiceAnnotation() {
        assertNotNull(TestService.class.getAnnotation(Service.class));
    }

    @Test
    void shouldDefineControllerAnnotation() {
        assertNotNull(TestController.class.getAnnotation(Controller.class));
    }

    @Component
    static class TestComponent {}

    @Repository
    static class TestRepository {}

    @Service
    static class TestService {}

    @Controller
    static class TestController {}
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=ComponentTest`
Expected: COMPILATION ERROR - 注解不存在

**Step 3: 创建@Component注解**

```java
package com.minispring.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件注解
 * 标识一个类为Spring管理的组件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

    /**
     * 组件名称
     * 如果未指定，将使用类名的首字母小写形式
     */
    String value() default "";
}
```

**Step 4: 创建@Repository注解**

```java
package com.minispring.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仓储注解
 * 标识一个类为数据访问层组件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {

    /**
     * 仓储名称
     */
    String value() default "";
}
```

**Step 5: 创建@Service注解**

```java
package com.minispring.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务注解
 * 标识一个类为服务层组件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {

    /**
     * 服务名称
     */
    String value() default "";
}
```

**Step 6: 创建@Controller注解**

```java
package com.minispring.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 控制器注解
 * 标识一个类为Web层控制器组件
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {

    /**
     * 控制器名称
     */
    String value() default "";
}
```

**Step 7: 运行测试**

Run: `mvn test -Dtest=ComponentTest`
Expected: 测试通过

**Step 8: 提交**

```bash
git add src/main/java/com/minispring/stereotype/ src/test/java/com/minispring/stereotype/
git commit -m "feat: 定义组件注解"
```

---

## 自动装配注解

### Task 2: 定义自动装配注解

**Files:**
- Create: `src/main/java/com/minispring/annotation/Autowired.java`
- Create: `src/main/java/com/minispring/annotation/Qualifier.java`
- Create: `src/main/java/com/minispring/annotation/Value.java`
- Test: `src/test/java/com/minispring/annotation/AutowiredTest.java`

**Step 1: 编写测试**

```java
package com.minispring.annotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AutowiredTest {

    @Test
    void shouldDefineAutowiredAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[0].getAnnotation(Autowired.class));
    }

    @Test
    void shouldDefineQualifierAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[0].getAnnotation(Qualifier.class));
    }

    @Test
    void shouldDefineValueAnnotation() {
        assertNotNull(TestService.class.getDeclaredFields()[1].getAnnotation(Value.class));
    }

    static class TestService {
        @Autowired
        @Qualifier("repository")
        private Repository repository;

        @Value("${app.name}")
        private String appName;
    }

    interface Repository {}
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=AutowiredTest`
Expected: COMPILATION ERROR

**Step 3: 创建@Autowired注解**

```java
package com.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动装配注解
 * 标识需要自动注入的依赖
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

    /**
     * 是否必须
     * 如果为true，依赖找不到时会抛出异常
     */
    boolean required() default true;
}
```

**Step 4: 创建@Qualifier注解**

```java
package com.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限定符注解
 * 用于消除依赖注入的歧义
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Qualifier {

    /**
     * 限定符值
     */
    String value();
}
```

**Step 5: 创建@Value注解**

```java
package com.minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 值注解
 * 用于注入外部配置值
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {

    /**
     * 属性值表达式
     * 例如: ${app.name} 或 ${app.name:default}
     */
    String value();
}
```

**Step 6: 运行测试**

Run: `mvn test -Dtest=AutowiredTest`
Expected: 测试通过

**Step 7: 提交**

```bash
git add src/main/java/com/minispring/annotation/ src/test/java/com/minispring/annotation/
git commit -m "feat: 定义自动装配注解"
```

---

## 组件扫描器

### Task 3: 实现ClassPathBeanScanner

**Files:**
- Create: `src/main/java/com/minispring/scanner/ClassPathScanner.java`
- Create: `src/main/java/com/minispring/scanner/ClassPathBeanScanner.java`
- Test: `src/test/java/com/minispring/scanner/ScannerTest.java`

**Step 1: 编写测试 - 定义扫描器行为**

```java
package com.minispring.scanner;

import com.minispring.stereotype.Component;
import com.minispring.stereotype.Repository;
import com.minispring.stereotype.Service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

public class ScannerTest {

    @Test
    void shouldScanComponents() {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner("com.minispring.scanner.test");
        
        Set<Class<?>> components = scanner.scan();
        
        assertTrue(components.contains(Component1.class));
        assertTrue(components.contains(Service1.class));
        assertTrue(components.contains(Repository1.class));
    }

    @Test
    void shouldGenerateBeanName() {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner();
        
        assertEquals("component1", scanner.generateBeanName(Component1.class));
        assertEquals("myComponent", scanner.generateBeanName(MyComponent.class));
    }

    @Component
    static class Component1 {}

    @Service
    static class Service1 {}

    @Repository
    static class Repository1 {}

    @Component("myComponent")
    static class MyComponent {}
}
```

**Step 2: 创建ClassPathScanner**

```java
package com.minispring.scanner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 类路径扫描器
 * 扫描指定包下的所有类
 */
public class ClassPathScanner {

    private final String basePackage;

    public ClassPathScanner(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * 扫描指定包下的所有类
     */
    public List<Class<?>> scan() {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            String packagePath = basePackage.replace('.', '/');
            Enumeration<URL> resources = getClass().getClassLoader().getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url.getProtocol().equals("file")) {
                    findClasses(new File(url.getFile()), basePackage, classes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan package: " + basePackage, e);
        }
        
        return classes;
    }

    /**
     * 递归查找类文件
     */
    private void findClasses(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                findClasses(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Skip class
                }
            }
        }
    }
}
```

**Step 3: 创建ClassPathBeanScanner**

```java
package com.minispring.scanner;

import com.minispring.stereotype.Component;
import com.minispring.stereotype.Repository;
import com.minispring.stereotype.Service;
import com.minispring.stereotype.Controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类路径Bean扫描器
 * 扫描带有组件注解的类
 */
public class ClassPathBeanScanner {

    private final String basePackage;
    private final ClassPathScanner scanner;

    public ClassPathBeanScanner(String basePackage) {
        this.basePackage = basePackage;
        this.scanner = new ClassPathScanner(basePackage);
    }

    public ClassPathBeanScanner() {
        this("");
    }

    /**
     * 扫描所有带组件注解的类
     */
    public Set<Class<?>> scan() {
        Set<Class<?>> components = new HashSet<>();
        List<Class<?>> classes = scanner.scan();
        
        for (Class<?> clazz : classes) {
            if (isComponent(clazz)) {
                components.add(clazz);
            }
        }
        
        return components;
    }

    /**
     * 检查类是否是组件
     */
    private boolean isComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class) ||
               clazz.isAnnotationPresent(Repository.class) ||
               clazz.isAnnotationPresent(Service.class) ||
               clazz.isAnnotationPresent(Controller.class);
    }

    /**
     * 生成Bean名称
     */
    public String generateBeanName(Class<?> clazz) {
        // 尝试从注解获取名称
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        
        Service service = clazz.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }
        
        Repository repository = clazz.getAnnotation(Repository.class);
        if (repository != null && !repository.value().isEmpty()) {
            return repository.value();
        }
        
        Controller controller = clazz.getAnnotation(Controller.class);
        if (controller != null && !controller.value().isEmpty()) {
            return controller.value();
        }
        
        // 默认使用类名首字母小写
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
```

**Step 4: 创建测试包**

创建 `src/test/java/com/minispring/scanner/test/` 目录并添加测试组件：

```java
package com.minispring.scanner.test;

import com.minispring.stereotype.Component;

@Component
class TestComponent {
    public TestComponent() {}
}
```

**Step 5: 运行测试**

Run: `mvn test -Dtest=ScannerTest`
Expected: 测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/scanner/ src/test/java/com/minispring/scanner/
git commit -m "feat: 实现组件扫描器"
```

---

## 集成注解支持到容器

### Task 4: 扩展容器支持注解驱动

**Files:**
- Modify: `src/main/java/com/minispring/factory/DefaultBeanContainer.java`
- Modify: `src/test/java/com/minispring/factory/BeanContainerTest.java`

**Step 1: 添加组件扫描方法**

在 `DefaultBeanContainer` 中添加：

```java
import com.minispring.scanner.ClassPathBeanScanner;
import com.minispring.annotation.Autowired;
import com.minispring.annotation.Qualifier;
import java.lang.reflect.Field;

/**
 * 扫描指定包并注册所有组件
 *
 * @param basePackage 要扫描的包名
 * @return 注册的组件数量
 */
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

**Step 2: 添加字段注入支持**

```java
/**
 * 执行字段注入（@Autowired）
 */
private void performFieldInjection(Object bean) throws Exception {
    Class<?> clazz = bean.getClass();
    
    for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Autowired.class)) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            
            try {
                // 获取限定符
                String qualifier = null;
                if (field.isAnnotationPresent(Qualifier.class)) {
                    qualifier = field.getAnnotation(Qualifier.class).value();
                }
                
                // 解析依赖
                Object dependency;
                if (qualifier != null) {
                    dependency = getBean(qualifier);
                } else {
                    if (dependencyResolver == null) {
                        dependencyResolver = new DependencyResolver(this);
                    }
                    dependency = dependencyResolver.resolve(field.getType());
                }
                
                // 设置字段值
                field.setAccessible(true);
                field.set(bean, dependency);
            } catch (BeanNotFoundException e) {
                if (autowired.required()) {
                    throw e;
                }
                // 非必须的依赖，跳过
            }
        }
    }
}
```

**Step 3: 修改createBeanWithDependencies**

在 `performSetterInjection` 后添加：

```java
// 执行字段注入
performFieldInjection(bean);
```

**Step 4: 添加测试**

```java
@Test
void shouldSupportComponentScan() {
    DefaultBeanContainer container = new DefaultBeanContainer();
    
    // 扫描测试包
    int count = container.scanComponents("com.minispring.scanner.test");
    
    assertTrue(count > 0);
    
    // 获取扫描到的Bean
    Object bean = container.getBean("testComponent");
    assertNotNull(bean);
}
```

**Step 5: 运行测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 6: 提交**

```bash
git add src/main/java/com/minispring/factory/ src/test/java/com/minispring/factory/
git commit -m "feat: 集成注解驱动到容器"
```

---

## 属性值注入

### Task 5: 实现属性值注入

**Files:**
- Create: `src/main/java/com/minispring/env/Environment.java`
- Create: `src/main/java/com/minispring/env/StandardEnvironment.java`
- Create: `src/main/java/com/minispring/env/PropertySource.java`
- Test: `src/test/java/com/minispring/env/EnvironmentTest.java`

**Step 1: 编写测试**

```java
package com.minispring.env;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentTest {

    @Test
    void shouldResolveProperty() {
        Environment env = new StandardEnvironment();
        
        // 设置属性
        env.setProperty("app.name", "MiniSpring");
        
        assertEquals("MiniSpring", env.getProperty("app.name"));
        assertEquals("MiniSpring", env.resolvePlaceholders("${app.name}"));
    }

    @Test
    void shouldSupportDefaultValue() {
        Environment env = new StandardEnvironment();
        
        assertEquals("default", env.resolvePlaceholders("${missing.property:default}"));
    }
}
```

**Step 2: 创建Environment接口**

```java
package com.minispring.env;

/**
 * 环境抽象
 * 用于解析属性值
 */
public interface Environment {

    /**
     * 获取属性值
     */
    String getProperty(String key);

    /**
     * 获取属性值，带默认值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 设置属性值
     */
    void setProperty(String key, String value);

    /**
     * 解析占位符
     * 例如: ${app.name} -> 实际值
     */
    String resolvePlaceholders(String text);
}
```

**Step 3: 创建StandardEnvironment**

```java
package com.minispring.env;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标准环境实现
 */
public class StandardEnvironment implements Environment {

    private final Map<String, String> properties = new HashMap<>();
    private final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?\\}");

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public String resolvePlaceholders(String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);
            String value = getProperty(key, defaultValue != null ? defaultValue : "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
```

**Step 4: 创建PropertySource注解**

```java
package com.minispring.env;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 属性源注解
 * 标识属性文件位置
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertySource {

    /**
     * 属性文件位置
     */
    String[] value();
}
```

**Step 5: 集成到容器**

在 `DefaultBeanContainer` 中添加：

```java
import com.minispring.annotation.Value;
import com.minispring.env.Environment;
import com.minispring.env.StandardEnvironment;

private Environment environment;

/**
 * 设置环境
 */
public void setEnvironment(Environment environment) {
    this.environment = environment;
}

/**
 * 获取环境
 */
public Environment getEnvironment() {
    if (environment == null) {
        environment = new StandardEnvironment();
    }
    return environment;
}

/**
 * 执行@Value注入
 */
private void performValueInjection(Object bean) throws Exception {
    Class<?> clazz = bean.getClass();
    
    for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Value.class)) {
            Value value = field.getAnnotation(Value.class);
            String resolvedValue = getEnvironment().resolvePlaceholders(value.value());
            
            field.setAccessible(true);
            
            // 类型转换
            Object convertedValue = convertValue(resolvedValue, field.getType());
            field.set(bean, convertedValue);
        }
    }
}

/**
 * 类型转换
 */
private Object convertValue(String value, Class<?> targetType) {
    if (targetType == String.class) {
        return value;
    } else if (targetType == int.class || targetType == Integer.class) {
        return Integer.parseInt(value);
    } else if (targetType == long.class || targetType == Long.class) {
        return Long.parseLong(value);
    } else if (targetType == boolean.class || targetType == Boolean.class) {
        return Boolean.parseBoolean(value);
    } else if (targetType == double.class || targetType == Double.class) {
        return Double.parseDouble(value);
    }
    throw new IllegalArgumentException("Unsupported type: " + targetType);
}
```

**Step 6: 修改createBeanWithDependencies**

在 `performFieldInjection` 后添加：

```java
// 执行@Value注入
performValueInjection(bean);
```

**Step 7: 运行测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 8: 提交**

```bash
git add src/main/java/com/minispring/ src/test/java/com/minispring/
git commit -m "feat: 实现属性值注入"
```

---

## 更新示例应用

### Task 6: 更新示例应用使用注解

**Files:**
- Modify: `src/main/java/com/minispring/samples/Application.java`
- Create: `src/main/java/com/minispring/samples/components/`

**Step 1: 创建组件示例**

创建 `src/main/java/com/minispring/samples/components/UserRepository.java`:

```java
package com.minispring.samples.components;

import com.minispring.stereotype.Repository;

@Repository
public class UserRepository {
    public void save(String username) {
        System.out.println("   保存用户: " + username);
    }
}
```

创建 `src/main/java/com/minispring/samples/components/UserService.java`:

```java
package com.minispring.samples.components;

import com.minispring.annotation.Autowired;
import com.minispring.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void createUser(String username) {
        System.out.println("   创建用户: " + username);
        userRepository.save(username);
    }
}
```

**Step 2: 更新Application**

```java
package com.minispring.samples;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.components.UserService;

public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段4 - 注解驱动) ===");
        System.out.println();

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 扫描组件
        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.components");
        System.out.println("扫描到 " + count + " 个组件");

        // 获取并使用UserService
        System.out.println();
        System.out.println("--- 使用UserService ---");
        UserService userService = (UserService) container.getBean("userService");
        userService.createUser("赵六");

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
```

**Step 3: 运行示例应用**

Run: `mvn compile exec:java -Dexec.mainClass="com.minispring.samples.Application"`
Expected: 输出显示注解驱动正常工作

**Step 4: 提交**

```bash
git add src/main/java/com/minispring/samples/
git commit -m "feat: 更新示例应用使用注解驱动"
```

---

## 阶段4完成检查

### Task 7: 验证阶段4完成

**Files:**
- Create: `docs/phases/phase04-completion-checklist.md`

**Step 1: 创建完成检查清单**

```markdown
# 阶段4完成检查清单

## 功能验证

- [x] @Component组件注解
- [x] @Repository仓储注解
- [x] @Service服务注解
- [x] @Controller控制器注解
- [x] @Autowired自动装配注解
- [x] @Qualifier限定符注解
- [x] @Value值注解
- [x] 组件扫描功能
- [x] 字段注入支持
- [x] 属性值注入

## 测试覆盖

- [x] 组件注解测试
- [x] 自动装配注解测试
- [x] 扫描器测试
- [x] 环境测试
- [x] 集成测试

## 代码质量

- [x] 所有测试通过
- [x] 代码有完整注释
- [x] 符合Java命名规范
- [x] 异常处理完善

## 学习目标达成

- [x] 理解注解驱动开发
- [x] 理解组件扫描原理
- [x] 理解反射在注解处理中的应用
- [x] 理解依赖注入的注解方式
```

**Step 2: 运行所有测试**

Run: `mvn test`
Expected: 所有测试通过

**Step 3: 创建阶段4标签**

```bash
git tag phase-4-completed
```

**Step 4: 提交完成文档**

```bash
git add docs/phases/phase04-completion-checklist.md
git commit -m "docs: 完成阶段4检查清单"
```

---

## 阶段4总结

完成本阶段后，你已经实现：
1. 完整的组件注解体系
2. 自动装配注解支持
3. 组件扫描功能
4. 属性值注入

**下一步：** 进入阶段5，实现AOP
