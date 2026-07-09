package com.minispring.transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 可观测的简单事务管理器（逻辑事务，无真实 DB）
 *
 * 用 ThreadLocal 维护当前线程的活动事务栈与事件日志：
 * - REQUIRED：栈非空则加入（不发 BEGIN），栈空则新建并入栈（发 BEGIN）
 * - REQUIRES_NEW：总是新建并入栈（发 BEGIN），外层留在栈下方（逻辑挂起）
 * - commit/rollback：isNew 则记 COMMIT/ROLLBACK 并出栈；加入则 no-op（commit）或标记外层 rollbackOnly（rollback）
 */
public class SimpleTransactionManager implements PlatformTransactionManager {

    private final ThreadLocal<Deque<SimpleTransactionStatus>> activeTxs =
            ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<List<String>> events =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        Deque<SimpleTransactionStatus> stack = activeTxs.get();
        if (propagation == Propagation.REQUIRED && !stack.isEmpty()) {
            // 加入现有事务
            return new SimpleTransactionStatus(false);
        }
        // 新建事务（REQUIRED 空栈 或 REQUIRES_NEW）
        SimpleTransactionStatus status = new SimpleTransactionStatus(true);
        stack.push(status);
        record("BEGIN");
        return status;
    }

    @Override
    public void commit(TransactionStatus status) {
        if (status.isRollbackOnly()) {
            doRollback(status);
            return;
        }
        if (status.isNewTransaction()) {
            asStatus(status).markCompleted();
            record("COMMIT");
            pop(status);
        } else {
            // 加入事务：提交由最外层负责，此处 no-op
            asStatus(status).markCompleted();
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        doRollback(status);
    }

    private void doRollback(TransactionStatus status) {
        if (status.isNewTransaction()) {
            asStatus(status).markCompleted();
            record("ROLLBACK");
            pop(status);
        } else {
            // 加入事务：标记栈顶新事务（外层）rollbackOnly
            asStatus(status).markCompleted();
            Deque<SimpleTransactionStatus> stack = activeTxs.get();
            if (!stack.isEmpty()) {
                stack.peek().setRollbackOnly();
            }
        }
    }

    private void pop(TransactionStatus status) {
        Deque<SimpleTransactionStatus> stack = activeTxs.get();
        if (!stack.isEmpty() && stack.peek() == status) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            activeTxs.remove();
        }
    }

    private void record(String event) {
        events.get().add(event);
    }

    private SimpleTransactionStatus asStatus(TransactionStatus status) {
        return (SimpleTransactionStatus) status;
    }

    /**
     * 当前线程的事件序列副本（BEGIN/COMMIT/ROLLBACK），供测试/样例断言
     */
    public List<String> getEvents() {
        return new ArrayList<>(events.get());
    }
}
