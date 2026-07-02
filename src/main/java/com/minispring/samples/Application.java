package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.lifecycle.LoggingPostProcessor;
import com.minispring.samples.lifecycle.UserService;
import com.minispring.samples.repository.UserRepository;

/**
 * Mini-Spring示例应用 - 阶段3
 * 演示Bean生命周期管理
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段3 - 生命周期) ===");
        System.out.println();

        BeanContainer container = new DefaultBeanContainer();

        // 注册后处理器
        ((DefaultBeanContainer) container).registerBeanPostProcessor(new LoggingPostProcessor());

        // 注册Bean
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        System.out.println("--- 获取UserService Bean ---");
        UserService userService = (UserService) container.getBean("userService");

        System.out.println();
        System.out.println("--- 使用UserService ---");
        userService.createUser("王五");

        System.out.println();
        System.out.println("--- 销毁容器 ---");
        ((DefaultBeanContainer) container).destroy();

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
