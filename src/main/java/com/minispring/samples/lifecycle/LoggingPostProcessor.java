package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.BeanPostProcessor;

public class LoggingPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 前置处理: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        System.out.println("   [后处理器] 后置处理: " + beanName);
        return bean;
    }
}
