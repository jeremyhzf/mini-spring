package com.minispring.factory.dependency;

import java.util.HashSet;
import java.util.Set;

/**
 * 循环依赖检测器
 * 检测Bean创建过程中的循环依赖
 */
public class CircularDependencyDetector {

    /**
     * 正在创建中的Bean集合
     */
    private final Set<String> beansInCreation = new HashSet<>();

    /**
     * 开始创建Bean
     *
     * @param beanName Bean名称
     * @throws CircularDependencyException 如果检测到循环依赖
     */
    public void beforeCreation(String beanName) {
        if (beansInCreation.contains(beanName)) {
            throw new CircularDependencyException(
                "Circular dependency detected: " + beanName + " is already being created"
            );
        }
        beansInCreation.add(beanName);
    }

    /**
     * Bean创建完成
     *
     * @param beanName Bean名称
     */
    public void afterCreation(String beanName) {
        beansInCreation.remove(beanName);
    }

    /**
     * 循环依赖异常
     */
    public static class CircularDependencyException extends RuntimeException {
        public CircularDependencyException(String message) {
            super(message);
        }
    }
}
