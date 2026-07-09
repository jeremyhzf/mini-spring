package com.minispring.samples.annotation;

import com.minispring.env.StandardEnvironment;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段4 - 注解驱动开发示例
 * 演示：组件扫描、@Autowired、@Value
 */
public class AnnotationDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段4：注解驱动开发示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 配置环境，供 @Value 解析占位符
        StandardEnvironment environment = new StandardEnvironment();
        environment.setProperty("app.name", "Mini-Spring 演示应用");
        container.setEnvironment(environment);

        // 组件扫描：自动注册带 @Service / @Repository 的类
        int count = container.scanComponents("com.minispring.samples.annotation");
        System.out.println("组件扫描: 发现并注册 " + count + " 个组件");

        // 获取 UserService，观察 @Autowired（注入 UserRepository）与 @Value（注入 appName）
        System.out.println("\n--- 调用 UserService（自动注入已生效）---");
        UserService userService = (UserService) container.getBean("userService");
        userService.createUser("张三");

        System.out.println("\n=== 阶段4 示例结束 ===");
    }
}
