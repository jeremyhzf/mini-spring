package com.minispring.transaction;

import com.minispring.aop.DefaultAdvisor;
import com.minispring.aop.MethodMatcher;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @Transactional 与容器集成测试：addAdvisor + 接口代理 + 事务事件
 */
public class TransactionalContainerIntegrationTest {

    private AccountService setup(SimpleTransactionManager manager) {
        DefaultBeanContainer container = new DefaultBeanContainer();
        container.addAdvisor(new DefaultAdvisor(
                new TransactionInterceptor(manager),
                (MethodMatcher) (method, targetClass) ->
                        method.isAnnotationPresent(Transactional.class)
                                || targetClass.isAnnotationPresent(Transactional.class)
        ));
        container.registerBean("accountService", AccountServiceImpl.class);
        return (AccountService) container.getBean("accountService");
    }

    @Test
    void creditCommitsViaProxy() {
        SimpleTransactionManager manager = new SimpleTransactionManager();
        AccountService svc = setup(manager);

        svc.credit("A001", 100);

        assertEquals(List.of("BEGIN", "COMMIT"), manager.getEvents());
    }

    @Test
    void creditAndFailRollsBackViaProxy() {
        SimpleTransactionManager manager = new SimpleTransactionManager();
        AccountService svc = setup(manager);

        assertThrows(RuntimeException.class, () -> svc.creditAndFail("A001", 100));

        assertEquals(List.of("BEGIN", "ROLLBACK"), manager.getEvents());
    }

    @Test
    void beanIsProxied() {
        Object bean = setup(new SimpleTransactionManager());
        assertNotSame(AccountServiceImpl.class, bean.getClass());
        assertInstanceOf(AccountService.class, bean);
    }
}
