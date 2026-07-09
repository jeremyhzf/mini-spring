package com.minispring.condition;

import com.minispring.env.Environment;

/**
 * 条件求值上下文
 * 向 Condition 暴露 Environment 与被评估的候选类。
 */
public class ConditionContext {

    private final Environment environment;
    private final Class<?> candidate;

    public ConditionContext(Environment environment, Class<?> candidate) {
        this.environment = environment;
        this.candidate = candidate;
    }

    /**
     * 环境（用于按属性判断等）
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * 被评估的候选 Bean 类
     */
    public Class<?> getCandidate() {
        return candidate;
    }
}
