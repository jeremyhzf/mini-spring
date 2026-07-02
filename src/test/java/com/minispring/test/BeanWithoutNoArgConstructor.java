package com.minispring.test;

/**
 * 用于测试没有无参构造器的Bean
 */
public class BeanWithoutNoArgConstructor {

    private final String name;

    public BeanWithoutNoArgConstructor(String name) {
        this.name = name;
    }
}
