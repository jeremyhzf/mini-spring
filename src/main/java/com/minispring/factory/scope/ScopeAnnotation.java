package com.minispring.factory.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用域注解
 * 用于标识Bean的作用域
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopeAnnotation {

    /**
     * 作用域名称
     */
    String value() default "singleton";
}
