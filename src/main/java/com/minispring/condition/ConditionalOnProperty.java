package com.minispring.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内置条件：按 Environment 属性开关 Bean。
 * 自身被 @Conditional(OnPropertyCondition.class) 标注，
 * 靠 ConditionEvaluator 的元注解解析被识别。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
public @interface ConditionalOnProperty {

    /**
     * 属性名
     */
    String name();

    /**
     * 期望的属性值；为空表示只要属性存在即匹配
     */
    String havingValue() default "";

    /**
     * 属性缺失时是否视为匹配
     */
    boolean matchIfMissing() default false;
}
