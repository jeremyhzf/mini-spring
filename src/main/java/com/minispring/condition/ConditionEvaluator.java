package com.minispring.condition;

import com.minispring.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 条件求值器
 * 解析候选类上（含经组合注解间接标注的）所有 @Conditional，
 * 实例化其 Condition 并求 AND；全真才应注册。无任何 @Conditional 时直接注册。
 */
public class ConditionEvaluator {

    private final Environment environment;

    public ConditionEvaluator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 候选类是否应当注册
     */
    public boolean shouldRegister(Class<?> candidate) {
        Set<Class<? extends Condition>> conditionTypes = findConditions(candidate);
        if (conditionTypes.isEmpty()) {
            return true;
        }
        ConditionContext context = new ConditionContext(environment, candidate);
        for (Class<? extends Condition> type : conditionTypes) {
            Condition condition = instantiate(type);
            if (!condition.matches(context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 递归收集候选类上所有可达 @Conditional 的 Condition 类型（含元注解链）
     */
    private Set<Class<? extends Condition>> findConditions(Class<?> candidate) {
        Set<Class<? extends Condition>> result = new LinkedHashSet<>();
        Set<Class<? extends Annotation>> seen = new HashSet<>();
        collect(candidate, result, seen);
        return result;
    }

    private void collect(AnnotatedElement element,
                         Set<Class<? extends Condition>> result,
                         Set<Class<? extends Annotation>> seen) {
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            // 每个 @Conditional（直接标注或经元注解）都各自携带 value，
            // 必须独立收集，故不参与 seen 去重；seen 仅用于防止元注解链成环
            if (type == Conditional.class) {
                // 直接标注的 @Conditional
                result.add(((Conditional) annotation).value());
                continue;
            }
            // 仅对非 Conditional 的注解类型做去重，避免 @Target/@Retention 等成环
            if (!seen.add(type)) {
                continue;
            }
            // 间接：该注解类型上是否元标注了 @Conditional
            Conditional meta = type.getDeclaredAnnotation(Conditional.class);
            if (meta != null) {
                result.add(meta.value());
            }
            // 继续向上递归，处理更深的组合注解
            collect(type, result, seen);
        }
    }

    private Condition instantiate(Class<? extends Condition> type) {
        try {
            Constructor<? extends Condition> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("无法实例化 Condition: " + type.getName(), e);
        }
    }
}
