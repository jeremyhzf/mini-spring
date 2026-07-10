package com.minispring.samples.transaction;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.transaction.SimpleTransactionManager;
import com.minispring.transaction.Transactional;
import com.minispring.transaction.TransactionInterceptor;

/**
 * 阶段7-5 - 声明式事务示例
 * 演示：@Transactional 接口方法经 AOP 代理驱动事务边界（提交/回滚），通过事件日志直观展示
 */
public class TransactionalDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-5：声明式事务 @Transactional 示例 ===\n");

        SimpleTransactionManager manager = new SimpleTransactionManager();

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(manager),
                (MethodMatcher) (method, targetClass) ->
                        method.isAnnotationPresent(Transactional.class)
                                || targetClass.isAnnotationPresent(Transactional.class)
        ));
        container.registerBean("transferService", TransferServiceImpl.class);
        TransferService service = (TransferService) container.getBean("transferService");

        System.out.println("--- 正常转账（提交）---");
        service.transfer("Alice", "Bob", 100);
        System.out.println("事务事件: " + manager.getEvents());

        System.out.println("\n--- 转账失败（回滚）---");
        try {
            service.transferAndFail("Bob", "Carol", 50);
        } catch (RuntimeException e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
        System.out.println("事务事件: " + manager.getEvents());

        System.out.println("\n=== 阶段7-5 示例结束 ===");
    }
}
