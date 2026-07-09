package com.minispring.condition;

import com.minispring.env.StandardEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ConditionalOnProperty 属性匹配测试
 */
public class OnPropertyConditionTest {

    @ConditionalOnProperty(name = "feature.x", havingValue = "true")
    static class MatchByValue {
    }

    @ConditionalOnProperty(name = "feature.x")  // havingValue 为空：只要属性存在即匹配
    static class MatchByPresence {
    }

    @ConditionalOnProperty(name = "feature.x", matchIfMissing = true)
    static class MatchIfMissing {
    }

    @ConditionalOnProperty(name = "feature.x", havingValue = "true")
    static class MissingNoFallback {
    }

    private boolean evaluate(Class<?> candidate, String propertyValue) {
        StandardEnvironment env = new StandardEnvironment();
        if (propertyValue != null) {
            env.setProperty("feature.x", propertyValue);
        }
        ConditionEvaluator evaluator = new ConditionEvaluator(env);
        return evaluator.shouldRegister(candidate);
    }

    @Test
    void shouldMatchWhenValueEquals() {
        assertTrue(evaluate(MatchByValue.class, "true"));
    }

    @Test
    void shouldNotMatchWhenValueDiffers() {
        assertFalse(evaluate(MatchByValue.class, "false"));
    }

    @Test
    void shouldMatchByPresenceWhenHavingValueEmpty() {
        assertTrue(evaluate(MatchByPresence.class, "anything"));
    }

    @Test
    void shouldNotMatchWhenAbsentAndNoFallback() {
        assertFalse(evaluate(MissingNoFallback.class, null));
    }

    @Test
    void shouldMatchWhenAbsentAndMatchIfMissing() {
        assertTrue(evaluate(MatchIfMissing.class, null));
    }

    @Test
    void shouldNotMatchByPresenceWhenAbsent() {
        assertFalse(evaluate(MatchByPresence.class, null));
    }
}
