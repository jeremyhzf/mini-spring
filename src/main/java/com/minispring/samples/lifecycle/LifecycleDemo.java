package com.minispring.samples.lifecycle;

import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段3 - Bean 生命周期与作用域示例
 * 演示：初始化/销毁回调、BeanPostProcessor、prototype 作用域
 */
public class LifecycleDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段3：Bean 生命周期与作用域示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 注册后处理器
        container.registerBeanPostProcessor(new LifecyclePostProcessor());

        // 注册并获取 LifecycleBean，观察初始化回调与后处理器介入
        System.out.println("--- 生命周期回调 + 后处理器 ---");
        container.registerBean("lifecycleBean", LifecycleBean.class);
        container.getBean("lifecycleBean");

        // prototype 作用域：每次获取都是新实例
        System.out.println("\n--- prototype 作用域 ---");
        container.registerBean("prototypeBean", PrototypeBean.class);
        PrototypeBean p1 = (PrototypeBean) container.getBean("prototypeBean");
        PrototypeBean p2 = (PrototypeBean) container.getBean("prototypeBean");
        System.out.println("两次获取 prototype Bean 为同一实例? "
                + (p1 == p2 ? "是（错误）" : "否（每次新建 ✓）"));

        // 销毁回调
        System.out.println("\n--- 销毁回调 ---");
        container.destroy();

        System.out.println("\n=== 阶段3 示例结束 ===");
    }
}
