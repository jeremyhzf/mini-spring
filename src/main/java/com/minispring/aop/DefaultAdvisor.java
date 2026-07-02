package com.minispring.aop;

import com.minispring.aop.advice.Advice;

/**
 * 默认的通知器实现
 */
public class DefaultAdvisor implements Advisor {

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
