package com.minispring.samples.ioc;

import com.minispring.factory.BeanNotFoundException;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段1 - Bean 容器示例
 * 演示：注册/获取 Bean、单例、BeanNotFoundException
 */
public class BeanContainerDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段1：Bean 容器示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 1. 注册 Bean
        container.registerBean("greeting", GreetingBean.class);

        // 2. 获取 Bean 并使用
        GreetingBean greeting = (GreetingBean) container.getBean("greeting");
        greeting.greet("世界");

        // 3. 单例验证：同名多次获取返回同一实例
        GreetingBean again = (GreetingBean) container.getBean("greeting");
        System.out.println("\n单例验证: " + (greeting == again
                ? "两次获取为同一实例 ✓"
                : "不同实例 ✗"));

        // 4. 异常演示：获取不存在的 Bean
        System.out.println("\n异常演示: 获取不存在的 Bean...");
        try {
            container.getBean("notExist");
        } catch (BeanNotFoundException e) {
            System.out.println("捕获 BeanNotFoundException: " + e.getMessage());
        }

        System.out.println("\n=== 阶段1 示例结束 ===");
    }
}
