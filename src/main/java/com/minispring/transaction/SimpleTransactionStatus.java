package com.minispring.transaction;

/**
 * 事务状态默认实现（包级可见，仅 SimpleTransactionManager 使用）
 */
class SimpleTransactionStatus implements TransactionStatus {

    private final boolean newTransaction;
    private boolean rollbackOnly;
    private boolean completed;

    SimpleTransactionStatus(boolean newTransaction) {
        this.newTransaction = newTransaction;
    }

    @Override
    public boolean isNewTransaction() {
        return newTransaction;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    void markCompleted() {
        if (completed) {
            throw new IllegalStateException("事务已完成，不能重复提交/回滚");
        }
        this.completed = true;
    }
}
