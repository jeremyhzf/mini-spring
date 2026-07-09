package com.minispring.samples.conditional;

import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段7-2 - 条件装配示例
 * 演示：@ConditionalOnProperty 按环境属性开关 Bean
 */
public class ConditionalDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-2：条件装配示例 ===\n");

        // 场景1：未设置属性 → 条件 Bean 不注册
        System.out.println("--- 场景1：未启用 premium（feature.premium 未设置）---");
        DefaultBeanContainer c1 = new DefaultBeanContainer();
        c1.scanComponents("com.minispring.samples.conditional");
        report(c1);

        // 场景2：设置属性 → 条件 Bean 注册
        System.out.println("\n--- 场景2：启用 premium（feature.premium=true）---");
        DefaultBeanContainer c2 = new DefaultBeanContainer();
        c2.getEnvironment().setProperty("feature.premium", "true");
        c2.scanComponents("com.minispring.samples.conditional");
        report(c2);

        System.out.println("\n=== 阶段7-2 示例结束 ===");
    }

    private static void report(DefaultBeanContainer container) {
        printBean(container, "basicService");
        printBean(container, "premiumService");
    }

    private static void printBean(DefaultBeanContainer container, String name) {
        try {
            container.getBean(name);
            System.out.println(name + ": 已注册");
        } catch (Exception e) {
            System.out.println(name + ": 未注册（条件不满足）");
        }
    }
}
