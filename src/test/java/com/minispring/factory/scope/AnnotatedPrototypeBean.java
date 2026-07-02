package com.minispring.factory.scope;

/**
 * 标记为原型作用域的测试Bean
 */
@ScopeAnnotation("prototype")
public class AnnotatedPrototypeBean {
    public AnnotatedPrototypeBean() {}
}
