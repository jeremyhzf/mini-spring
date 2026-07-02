package com.minispring.factory;

/**
 * Bean容器接口，定义注册和获取Bean的基本操作
 *
 * @author mini-spring
 * @since 1.0.0
 */
public interface BeanContainer {

    /**
     * 注册Bean定义
     *
     * @param name Bean的名称
     * @param clazz Bean的类型
     */
    void registerBean(String name, Class<?> clazz);

    /**
     * 获取Bean实例
     *
     * @param name Bean的名称
     * @return Bean实例
     * @throws BeanNotFoundException 当Bean不存在时抛出
     */
    Object getBean(String name) throws BeanNotFoundException;
}
