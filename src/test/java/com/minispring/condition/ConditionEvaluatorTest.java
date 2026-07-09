package com.minispring.condition;

import com.minispring.env.Environment;
import com.minispring.env.StandardEnvironment;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件求值器测试：无注解、直接标注、元注解解析、AND 语义
 */
public class ConditionEvaluatorTest {

    /** 恒真条件 */
    public static class TrueCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            return true;
        }
    }

    /** 恒假条件 */
    public static class FalseCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context) {
            return false;
        }
    }

    /** 组合注解：自身携带 @Conditional(TrueCondition.class) */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Conditional(TrueCondition.class)
    public @interface ComposedTrue {
    }

    static class NoCondition {
    }

    @Conditional(TrueCondition.class)
    static class DirectTrue {
    }

    @Conditional(FalseCondition.class)
    static class DirectFalse {
    }

    @ComposedTrue
    static class MetaTrue {
    }

    /** 经组合注解带 TrueCondition，又直接标 FalseCondition —— AND 应为 false */
    @ComposedTrue
    @Conditional(FalseCondition.class)
    static class AndCase {
    }

    private ConditionEvaluator evaluator() {
        Environment env = new StandardEnvironment();
        return new ConditionEvaluator(env);
    }

    @Test
    void shouldRegisterWhenNoConditional() {
        assertTrue(evaluator().shouldRegister(NoCondition.class), "无 @Conditional 应注册");
    }

    @Test
    void shouldRegisterWhenDirectConditionTrue() {
        assertTrue(evaluator().shouldRegister(DirectTrue.class));
    }

    @Test
    void shouldSkipWhenDirectConditionFalse() {
        assertFalse(evaluator().shouldRegister(DirectFalse.class));
    }

    @Test
    void shouldResolveConditionViaMetaAnnotation() {
        assertTrue(evaluator().shouldRegister(MetaTrue.class),
            "应透过组合注解 @ComposedTrue 解析到 @Conditional(TrueCondition)");
    }

    @Test
    void shouldAndMultipleReachableConditions() {
        assertFalse(evaluator().shouldRegister(AndCase.class),
            "多个可达 Condition 取 AND，其一为 false 则跳过");
    }

    @Test
    void conditionContextShouldExposeEnvironmentAndCandidate() {
        Environment env = new StandardEnvironment();
        ConditionContext ctx = new ConditionContext(env, DirectTrue.class);
        assertTrue(ctx.getEnvironment() == env, "应暴露构造时传入的 Environment");
        assertTrue(ctx.getCandidate() == DirectTrue.class, "应暴露候选类");
    }
}
