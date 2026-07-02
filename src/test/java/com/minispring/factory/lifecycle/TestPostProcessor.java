package com.minispring.factory.lifecycle;

public class TestPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        if (bean instanceof LifecycleBean) {
            ((LifecycleBean) bean).setStatus("post-processed");
        }
        return bean;
    }
}
