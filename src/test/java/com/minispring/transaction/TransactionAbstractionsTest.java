package com.minispring.transaction;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务抽象契约测试：注解默认值、枚举、接口方法存在性
 */
public class TransactionAbstractionsTest {

    public static class Fixture {
        @Transactional
        public void defaultTx() {
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = IllegalStateException.class)
        public void customTx() {
        }
    }

    @Test
    void transactionalDefaultsAreRequiredAndEmpty() throws NoSuchMethodException {
        Transactional t = Fixture.class.getMethod("defaultTx").getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRED, t.propagation(), "默认传播应为 REQUIRED");
        assertEquals(0, t.rollbackFor().length, "默认 rollbackFor 为空");
        assertEquals(0, t.noRollbackFor().length, "默认 noRollbackFor 为空");
    }

    @Test
    void transactionalCustomAttributes() throws NoSuchMethodException {
        Transactional t = Fixture.class.getMethod("customTx").getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, t.propagation());
        assertEquals(IllegalStateException.class, t.rollbackFor()[0]);
    }

    @Test
    void propagationHasRequiredAndRequiresNew() {
        assertTrue(Arrays.asList(Propagation.values()).contains(Propagation.REQUIRED));
        assertTrue(Arrays.asList(Propagation.values()).contains(Propagation.REQUIRES_NEW));
    }
}
