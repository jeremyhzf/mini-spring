package com.minispring.samples.ioc;

/**
 * 阶段1 演示用简单 Bean
 */
public class GreetingBean {

    public void greet(String name) {
        System.out.println("你好，" + name + "！我是来自 Mini-Spring 容器的 Bean。");
    }
}
