package com.minispring.factory;

/**
 * Bean找不到异常
 */
public class BeanNotFoundException extends RuntimeException {

    private final String beanName;

    public BeanNotFoundException(String beanName) {
        super("Bean not found: " + beanName);
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
