package com.minispring.event;

import com.minispring.factory.lifecycle.BeanPostProcessor;

/**
 * 监听器探测器（后处理器）
 * 在 Bean 初始化完成后，若该 Bean 实现了 ApplicationListener，则注册进多播器。
 * 容器在构造时自动注册本探测器，用户无需手动登记监听器。
 */
public class ApplicationListenerDetector implements BeanPostProcessor {

    private final ApplicationEventMulticaster multicaster;

    public ApplicationListenerDetector(ApplicationEventMulticaster multicaster) {
        this.multicaster = multicaster;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (bean instanceof ApplicationListener<?> listener) {
            multicaster.addApplicationListener(listener);
        }
        return bean;
    }
}
