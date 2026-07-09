package com.minispring.aop;

import com.minispring.aop.advice.Advice;

/**
 * 默认的通知器实现
 */
public class DefaultAdvisor implements Advisor {

    // Advice（通知） 和 Pointcut（切入点）
    private final Advice advice;
    private final Pointcut pointcut;

    public DefaultAdvisor(Advice advice, Pointcut pointcut) {
        this.advice = advice;
        this.pointcut = pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }
}
