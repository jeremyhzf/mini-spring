package com.minispring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求映射注解
 * 用于将HTTP请求映射到处理器方法
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

    /**
     * 请求路径
     */
    String value() default "";

    /**
     * 请求方法
     */
    RequestMethod[] method() default {};

    /**
     * 请求方法枚举
     */
    enum RequestMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE
    }
}
