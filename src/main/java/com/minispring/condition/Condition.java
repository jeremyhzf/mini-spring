package com.minispring.condition;

/**
 * 条件接口
 * 实现类在 ConditionContext 上判断是否应当注册候选 Bean。
 */
public interface Condition {

    /**
     * 是否满足条件
     *
     * @param context 条件上下文（Environment + 候选类）
     * @return true 表示满足，候选 Bean 应被注册
     */
    boolean matches(ConditionContext context);
}
