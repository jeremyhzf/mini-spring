package com.minispring.factory.lifecycle;

/**
 * Bean初始化接口
 * Bean实现此接口后，在属性设置完成后会调用afterPropertiesSet方法
 */
public interface InitializingBean {

    /**
     * 在Bean的所有属性设置完成后调用
     * 用于执行初始化逻辑
     */
    void afterPropertiesSet();
}
