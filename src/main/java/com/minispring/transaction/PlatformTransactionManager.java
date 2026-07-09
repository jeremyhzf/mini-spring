package com.minispring.transaction;

/**
 * 事务管理器抽象
 * 真实实现可对接 JDBC Connection / DataSource；本框架的 SimpleTransactionManager 为逻辑事务。
 */
public interface PlatformTransactionManager {

    /** 按传播行为获取（开启或加入）事务 */
    TransactionStatus getTransaction(Propagation propagation);

    /** 提交事务 */
    void commit(TransactionStatus status);

    /** 回滚事务 */
    void rollback(TransactionStatus status);
}
