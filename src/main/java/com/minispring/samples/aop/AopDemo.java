package com.minispring.samples.aop;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.aop.interceptor.LoggingInterceptor;
import com.minispring.aop.interceptor.TransactionInterceptor;
import com.minispring.factory.DefaultBeanContainer;

/**
 * 阶段5 - AOP 面向切面编程示例
 * 演示：Advisor + 方法切点匹配、代理 Bean、拦截器执行顺序
 */
public class AopDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段5：AOP 面向切面编程示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 事务拦截器：只应用于 create 开头的方法
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(),
                // 方法匹配器, 匹配 create 开头的方法
                (MethodMatcher) (method, targetClass) -> method.getName().startsWith("create")
        ));

        // 日志拦截器：应用于所有方法
        container.addAdvisor(new DefaultAdvisor(
                new LoggingInterceptor(),
                // 方法匹配器, 匹配所有方法
                (MethodMatcher) (method, targetClass) -> true
        ));

        // 组件扫描
        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.aop");
        System.out.println("扫描到 " + count + " 个组件");

        // 获取 OrderService（AOP 代理）
        System.out.println("\n--- 创建订单（触发事务 + 日志）---");
        IOrderService orderService = (IOrderService) container.getBean("orderService");
        orderService.createOrder("ORD-001");

        System.out.println("\n--- 取消订单（仅触发日志）---");
        orderService.cancelOrder("ORD-001");

        System.out.println("\n=== 阶段5 示例结束 ===");
    }
}
