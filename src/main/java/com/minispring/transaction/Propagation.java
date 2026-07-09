package com.minispring.transaction;

/**
 * 事务传播行为
 */
public enum Propagation {
    /** 有则加入、无则新建（默认） */
    REQUIRED,
    /** 总是新建独立事务（挂起外层） */
    REQUIRES_NEW
}
