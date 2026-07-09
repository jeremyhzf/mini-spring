package com.minispring.transaction;

/**
 * 集成测试用接口：@Transactional 标在接口方法上
 */
public interface AccountService {

    @Transactional
    void credit(String account, int amount);

    @Transactional
    void creditAndFail(String account, int amount);
}
