package com.minispring.transaction;

/**
 * AccountService 实现（逻辑事务，无真实 DB 操作）
 */
public class AccountServiceImpl implements AccountService {

    @Override
    public void credit(String account, int amount) {
        // 业务逻辑占位：真实场景下这里会更新账户余额
    }

    @Override
    public void creditAndFail(String account, int amount) {
        throw new RuntimeException("入账失败");
    }
}
