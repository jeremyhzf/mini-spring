package com.minispring.samples;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.annotation.UserService;

/**
 * Mini-Spring示例应用 - 阶段4
 * 演示注解驱动开发
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段4 - 注解驱动) ===");
        System.out.println();

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 扫描组件
        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.annotation");
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
