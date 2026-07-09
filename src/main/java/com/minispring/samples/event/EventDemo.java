package com.minispring.samples.event;

import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段7-1 - 事件机制示例
 * 演示：发布-订阅、按类型路由监听器、容器就绪/关闭生命周期事件
 */
public class EventDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-1：事件机制示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.event");
        System.out.println("扫描到 " + count + " 个组件");

        System.out.println("\n--- 刷新容器（触发 ContextRefreshedEvent）---");
        container.refresh();

        System.out.println("\n--- 注册用户（发布自定义事件）---");
        UserService userService = (UserService) container.getBean("userService");
        userService.register("Alice");

        System.out.println("\n--- 关闭容器（触发 ContextClosedEvent）---");
        container.destroy();

        System.out.println("\n=== 阶段7-1 示例结束 ===");
    }
}
