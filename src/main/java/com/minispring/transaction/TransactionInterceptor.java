package com.minispring.transaction;

import com.minispring.aop.advice.AroundAdvice;
import com.minispring.aop.advice.MethodInvocation;

import java.lang.reflect.Method;

/**
 * 事务拦截器
 * 读 @Transactional → 由管理器开启/加入事务 → 执行目标方法 → 按规则提交/回滚。
 * 全限定名 com.minispring.transaction.TransactionInterceptor；
 * 与阶段 5 的 com.minispring.aop.interceptor.TransactionInterceptor（打印桩）同名不同包。
 */
public class TransactionInterceptor implements AroundAdvice {

    private final PlatformTransactionManager manager;

    public TransactionInterceptor(PlatformTransactionManager manager) {
        this.manager = manager;
    }

    @Override
    public Object around(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional tx = resolveTransactional(method, invocation.getTarget().getClass());
        if (tx == null) {
            return invocation.proceed();
        }

        TransactionStatus status = manager.getTransaction(tx.propagation());
        try {
            Object result = invocation.proceed();
            manager.commit(status);
            return result;
        } catch (Throwable t) {
            if (shouldRollbackOn(tx, t)) {
                manager.rollback(status);
            } else {
                manager.commit(status);
            }
            throw t;
        }
    }

    /** 先方法（接口方法）后类（impl）查找 @Transactional */
    private Transactional resolveTransactional(Method method, Class<?> targetClass) {
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx != null) {
            return tx;
        }
        return targetClass.getAnnotation(Transactional.class);
    }

    /** 回滚判定：noRollbackFor 优先；rollbackFor 非空则只对命中类型回滚；否则默认 RuntimeException/Error 回滚 */
    private boolean shouldRollbackOn(Transactional tx, Throwable t) {
        for (Class<? extends Throwable> noRollback : tx.noRollbackFor()) {
            if (noRollback.isInstance(t)) {
                return false;
            }
        }
        if (tx.rollbackFor().length > 0) {
            for (Class<? extends Throwable> rollback : tx.rollbackFor()) {
                if (rollback.isInstance(t)) {
                    return true;
                }
            }
            return false;
        }
        return (t instanceof RuntimeException || t instanceof Error);
    }
}
