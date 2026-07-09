package com.minispring.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件装配注解
 * 标注在组件类上；扫描时由 ConditionEvaluator 求解其 Condition，
 * 全部 matches 为 true 才注册该 Bean。可经组合注解间接标注（元注解）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Conditional {

    /**
     * 条件实现类（需有无参构造）
     */
    Class<? extends Condition> value();
}
