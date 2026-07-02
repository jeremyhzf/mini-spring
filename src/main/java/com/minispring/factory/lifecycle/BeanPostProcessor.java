package com.minispring.factory.lifecycle;

/**
 * Bean后处理器接口
 * 允许在Bean初始化前后对Bean进行自定义处理
 */
public interface BeanPostProcessor {

    /**
     * 在Bean初始化前调用
     *
     * @param beanName Bean名称
     * @param bean Bean实例
     * @return 处理后的Bean实例
     */
    default Object postProcessBeforeInitialization(String beanName, Object bean) {
        return bean;
    }

    /**
     * 在Bean初始化后调用
     *
     * @param beanName Bean名称
     * @param bean Bean实例
     * @return 处理后的Bean实例
     */
    default Object postProcessAfterInitialization(String beanName, Object bean) {
        return bean;
    }
}
