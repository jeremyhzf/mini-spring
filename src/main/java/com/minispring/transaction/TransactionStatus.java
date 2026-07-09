package com.minispring.transaction;

/**
 * 事务状态
 */
public interface TransactionStatus {

    /** 是否为新建的事务（非加入） */
    boolean isNewTransaction();

    /** 是否仅回滚 */
    boolean isRollbackOnly();

    /** 标记为仅回滚 */
    void setRollbackOnly();

    /** 是否已完成（commit/rollback） */
    boolean isCompleted();
}
