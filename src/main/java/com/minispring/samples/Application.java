package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.repository.UserRepository;
import com.minispring.samples.service.UserService;

/**
 * Mini-Spring示例应用 - 阶段2
 * 演示自动依赖注入
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段2 - 依赖注入) ===");
        System.out.println();

        // 创建容器
        BeanContainer container = new DefaultBeanContainer();

        // 注册Bean（注意：先注册依赖，后注册使用方）
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        // 获取UserService，依赖会自动注入
        UserService userService = (UserService) container.getBean("userService");

        // 使用Bean
        System.out.println("调用UserService:");
        userService.createUser("李四");

        System.out.println();
        System.out.println("=== 依赖注入自动完成！===");
        System.out.println("=== 应用运行完成 ===");
    }
}
