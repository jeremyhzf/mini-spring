package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;

/**
 * Bean作用域接口
 * 定义Bean的实例化策略
 */
public interface Scope {

    /**
     * 获取作用域名称
     */
    String getName();

    /**
     * 获取该作用域下的Bean实例
     *
     * @param beanName Bean名称
     * @param beanFactory Bean工厂
     * @param beanCreator Bean创建器
     * @return Bean实例
     */
    Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator);

    /**
     * Bean创建器函数式接口
     */
    @FunctionalInterface
    interface BeanCreator {
        Object create() throws Exception;
    }
}
