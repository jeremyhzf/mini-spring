package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;

/**
 * 原型作用域
 * 每次请求都创建新实例
 */
public class PrototypeScope implements Scope {

    @Override
    public String getName() {
        return "prototype";
    }

    @Override
    public Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator) {
        try {
            return beanCreator.create();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create prototype bean: " + beanName, e);
        }
    }
}
