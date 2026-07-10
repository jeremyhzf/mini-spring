package com.minispring.samples.transaction;

/**
 * 转账服务实现（逻辑事务，无真实 DB）
 */
public class TransferServiceImpl implements TransferService {

    @Override
    public void transfer(String from, String to, int amount) {
        System.out.println("[业务] " + from + " -> " + to + " : " + amount);
    }

    @Override
    public void transferAndFail(String from, String to, int amount) {
        System.out.println("[业务] " + from + " -> " + to + " : " + amount + "（将失败）");
        throw new RuntimeException("转账失败");
    }
}
