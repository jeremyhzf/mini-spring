package com.minispring.condition;

/**
 * @ConditionalOnProperty 的条件实现
 * 从候选类读取直接标注的 @ConditionalOnProperty，按 Environment 属性判断。
 */
public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context) {
        ConditionalOnProperty annotation = context.getCandidate().getAnnotation(ConditionalOnProperty.class);
        String value = context.getEnvironment().getProperty(annotation.name());

        if (value == null) {
            // 属性缺失
            return annotation.matchIfMissing();
        }
        if (annotation.havingValue().isEmpty()) {
            // 只要属性存在即匹配
            return true;
        }
        return annotation.havingValue().equals(value);
    }
}
