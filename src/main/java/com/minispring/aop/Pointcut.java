package com.minispring.aop;

/**
 * 切点接口
 * 定义连接点的匹配规则
 */
public interface Pointcut {

    /**
     * 获取类过滤器
     */
    ClassFilter getClassFilter();

    /**
     * 方法匹配器
     */
    MethodMatcher getMethodMatcher();
}
