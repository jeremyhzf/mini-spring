package com.minispring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * POST请求映射注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(method = RequestMapping.RequestMethod.POST)
public @interface PostMapping {

    /**
     * 请求路径
     */
    String value() default "";
}
