package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.BeanPostProcessor;

/**
 * 演示 BeanPostProcessor 前后置介入
 */
public class LifecyclePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 初始化前: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 初始化后: " + beanName);
        return bean;
    }
}
