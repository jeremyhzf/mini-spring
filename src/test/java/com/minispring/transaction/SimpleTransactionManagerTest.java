package com.minispring.transaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleTransactionManager 测试：传播、commit/rollback、回滚传播、事件序列
 */
public class SimpleTransactionManagerTest {

    @Test
    void requiredStartsNewWhenNoneActive() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        assertTrue(s.isNewTransaction());
        assertEquals(List.of("BEGIN"), m.getEvents());
    }

    @Test
    void requiredJoinsWhenActive() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        m.getTransaction(Propagation.REQUIRED); // 外层 BEGIN
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        assertFalse(inner.isNewTransaction());
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入不再发 BEGIN");
    }

    @Test
    void requiresNewAlwaysNew() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        m.getTransaction(Propagation.REQUIRED); // 外层 BEGIN
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRES_NEW);
        assertTrue(inner.isNewTransaction());
        assertEquals(List.of("BEGIN", "BEGIN"), m.getEvents());
    }

    @Test
    void commitNewRecordsCommitAndPops() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        m.commit(s);
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents());
        // 提交后栈空，下一次 REQUIRED 应重新新建
        assertTrue(m.getTransaction(Propagation.REQUIRED).isNewTransaction());
    }

    @Test
    void commitJoinedIsNoOp() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        m.commit(inner);
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入事务提交为 no-op");
        assertFalse(outer.isCompleted(), "外层不应被内层提交完成");
    }

    @Test
    void rollbackNewRecordsRollback() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus s = m.getTransaction(Propagation.REQUIRED);
        m.rollback(s);
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void rollbackJoinedMarksOuterRollbackOnly() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRED); // 加入
        m.rollback(inner);
        assertEquals(List.of("BEGIN"), m.getEvents(), "加入事务回滚不立即记 ROLLBACK");
        assertTrue(outer.isRollbackOnly(), "外层应被标记 rollbackOnly");
        m.commit(outer); // 外层 rollbackOnly → 提交变回滚
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void requiresNewNestedCommitsIndependently() {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionStatus outer = m.getTransaction(Propagation.REQUIRED);
        TransactionStatus inner = m.getTransaction(Propagation.REQUIRES_NEW);
        m.commit(inner);
        m.commit(outer);
        assertEquals(List.of("BEGIN", "BEGIN", "COMMIT", "COMMIT"), m.getEvents());
        assertTrue(outer.isCompleted() && inner.isCompleted());
    }
}
