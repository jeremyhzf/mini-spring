package com.minispring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GET请求映射注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(method = RequestMapping.RequestMethod.GET)
public @interface GetMapping {

    /**
     * 请求路径
     */
    String value() default "";
}
