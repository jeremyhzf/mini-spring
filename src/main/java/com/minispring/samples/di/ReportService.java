package com.minispring.samples.di;

/**
 * Setter 注入示例：SetterInjector 通过 setXxx + 类型解析注入
 */
public class ReportService {

    private NotificationRepository repository;

    public void setNotificationRepository(NotificationRepository repository) {
        this.repository = repository;
    }

    public void sendReport(String report) {
        System.out.println("   [Setter注入] ReportService 发送报告");
        repository.save(report);
    }
}
