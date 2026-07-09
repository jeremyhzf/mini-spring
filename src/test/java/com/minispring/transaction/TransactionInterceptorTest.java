package com.minispring.transaction;

import com.minispring.aop.advice.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionInterceptor 测试：提交/回滚规则、rollbackFor/noRollbackFor、非事务方法
 */
public class TransactionInterceptorTest {

    public static class Fixture {
        @Transactional
        public String success() {
            return "ok";
        }

        @Transactional
        public String unchecked() {
            throw new RuntimeException("boom");
        }

        @Transactional
        public String checked() throws Exception {
            throw new Exception("checked");
        }

        @Transactional(rollbackFor = Exception.class)
        public String rollbackForChecked() throws Exception {
            throw new Exception("checked-rb");
        }

        @Transactional(noRollbackFor = RuntimeException.class)
        public String noRollbackUnchecked() {
            throw new RuntimeException("no-rb");
        }

        public String notTransactional() {
            return "plain";
        }
    }

    private MethodInvocation invocation(Object target, String name) throws Exception {
        Method method = target.getClass().getMethod(name);
        return new MethodInvocation() {
            @Override public Method getMethod() { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public Object getTarget() { return target; }
            @Override public Object proceed() throws Throwable {
                try {
                    return method.invoke(target);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };
    }

    @Test
    void successCommits() throws Throwable {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        Object result = it.around(invocation(new Fixture(), "success"));
        assertEquals("ok", result);
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents());
    }

    @Test
    void uncheckedExceptionRollsBack() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(RuntimeException.class, () -> it.around(invocation(new Fixture(), "unchecked")));
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void checkedExceptionCommitsByDefault() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(Exception.class, () -> it.around(invocation(new Fixture(), "checked")));
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents(), "受检异常默认提交");
    }

    @Test
    void rollbackForOverridesChecked() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(Exception.class, () -> it.around(invocation(new Fixture(), "rollbackForChecked")));
        assertEquals(List.of("BEGIN", "ROLLBACK"), m.getEvents());
    }

    @Test
    void noRollbackForSuppressesUnchecked() throws Exception {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        assertThrows(RuntimeException.class, () -> it.around(invocation(new Fixture(), "noRollbackUnchecked")));
        assertEquals(List.of("BEGIN", "COMMIT"), m.getEvents(), "noRollbackFor 覆盖默认回滚");
    }

    @Test
    void nonTransactionalMethodProceedsWithoutTransaction() throws Throwable {
        SimpleTransactionManager m = new SimpleTransactionManager();
        TransactionInterceptor it = new TransactionInterceptor(m);
        Object result = it.around(invocation(new Fixture(), "notTransactional"));
        assertEquals("plain", result);
        assertTrue(m.getEvents().isEmpty(), "非事务方法不应触发任何事务事件");
    }
}
