package com.minispring.aop;

import com.minispring.aop.advice.Advice;

/**
 * 通知器
 * 将增强和切点组合在一起
 */
public interface Advisor {

    /**
     * 获取增强
     */
    Advice getAdvice();

    /**
     * 获取切点
     */
    Pointcut getPointcut();

    /**
     * 判断是否为每个类创建一个代理实例（Per-Class）
     * 通常为true，除非是引入（Introduction）
     */
    default boolean isPerInstance() {
        return true;
    }
}
