package com.minispring.samples;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.repository.UserRepository;
import com.minispring.samples.service.UserService;

/**
 * Mini-Spring示例应用
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 ===");
        System.out.println();

        // 创建容器
        BeanContainer container = new DefaultBeanContainer();

        // 注册Bean
        container.registerBean("userRepository", UserRepository.class);
        container.registerBean("userService", UserService.class);

        // 获取Bean
        UserRepository userRepository = (UserRepository) container.getBean("userRepository");
        UserService userService = (UserService) container.getBean("userService");

        // 手动注入依赖（阶段2将实现自动注入）
        userService.setUserRepository(userRepository);

        // 使用Bean
        System.out.println("调用UserService:");
        userService.createUser("张三");

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
