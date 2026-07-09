package com.minispring.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式事务注解
 * 标在接口方法（或 impl 类）上；经 AOP 代理后由 TransactionInterceptor 驱动事务边界。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {

    /** 传播行为，默认 REQUIRED */
    Propagation propagation() default Propagation.REQUIRED;

    /** 触发回滚的异常类型（为空则用默认规则） */
    Class<? extends Throwable>[] rollbackFor() default {};

    /** 不触发回滚的异常类型（覆盖默认/rollbackFor） */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
