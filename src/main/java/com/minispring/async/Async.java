package com.minispring.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步注解（方法级）
 * 标在接口方法上；Bean 经 AOP 代理后，调用该方法会被提交到 Executor 异步执行。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
}
