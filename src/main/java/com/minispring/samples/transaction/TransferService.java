package com.minispring.samples.transaction;

import com.minispring.transaction.Transactional;

/**
 * 转账服务接口：@Transactional 标在接口方法上
 */
public interface TransferService {

    @Transactional
    void transfer(String from, String to, int amount);

    @Transactional
    void transferAndFail(String from, String to, int amount);
}
