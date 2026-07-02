package com.minispring.samples;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.aop.interceptor.LoggingInterceptor;
import com.minispring.aop.interceptor.TransactionInterceptor;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.samples.aop.IOrderService;

public class Application {

    public static void main(String[] args) {
        System.out.println("=== Mini-Spring 示例应用 (阶段5 - AOP) ===");
        System.out.println();

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 添加事务拦截器（只应用于create开头的方法）
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(),
                new MethodMatcher() {
                    @Override
                    public boolean matches(java.lang.reflect.Method method, Class<?> targetClass) {
                        return method.getName().startsWith("create");
                    }
                }
        ));

        // 添加日志拦截器（应用于所有方法）
        container.addAdvisor(new DefaultAdvisor(
                new LoggingInterceptor(),
                (MethodMatcher) (method, targetClass) -> true
        ));

        // 扫描组件
        System.out.println("--- 扫描组件 ---");
        int count = container.scanComponents("com.minispring.samples.aop");
        System.out.println("扫描到 " + count + " 个组件");

        // 获取并使用OrderService
        System.out.println();
        System.out.println("--- 创建订单（带事务）---");
        IOrderService orderService = (IOrderService) container.getBean("orderService");
        orderService.createOrder("ORD-001");

        System.out.println();
        System.out.println("--- 取消订单（不带事务）---");
        orderService.cancelOrder("ORD-001");

        System.out.println();
        System.out.println("=== 应用运行完成 ===");
    }
}
