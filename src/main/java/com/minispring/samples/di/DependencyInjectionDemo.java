package com.minispring.samples.di;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.factory.dependency.CircularDependencyDetector;

/**
 * 阶段2 - 依赖注入示例
 * 演示：构造器注入、Setter 注入、循环依赖检测
 */
public class DependencyInjectionDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段2：依赖注入示例 ===\n");

        DefaultBeanContainer container = new DefaultBeanContainer();

        // 注册名采用类名首字母小写，便于按类型解析
        container.registerBean("notificationRepository", NotificationRepository.class);
        container.registerBean("notificationService", NotificationService.class);
        container.registerBean("reportService", ReportService.class);

        // 构造器注入
        System.out.println("--- 构造器注入 ---");
        NotificationService notificationService =
                (NotificationService) container.getBean("notificationService");
        notificationService.sendNotification("订单已发货");

        // Setter 注入
        System.out.println("\n--- Setter 注入 ---");
        ReportService reportService = (ReportService) container.getBean("reportService");
        reportService.sendReport("月度销售报告");

        // 循环依赖检测
        System.out.println("\n--- 循环依赖检测 ---");
        container.registerBean("cycleA", CycleA.class);
        container.registerBean("cycleB", CycleB.class);
        try {
            container.getBean("cycleA");
        } catch (CircularDependencyDetector.CircularDependencyException e) {
            System.out.println("检测到循环依赖: " + e.getMessage());
        }

        System.out.println("\n=== 阶段2 示例结束 ===");
    }

    /** 循环依赖演示用嵌套类 */
    static class CycleA {
        public CycleA(CycleB b) {
        }
    }

    /** 循环依赖演示用嵌套类 */
    static class CycleB {
        public CycleB(CycleA a) {
        }
    }
}
