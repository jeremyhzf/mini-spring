package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例作用域
 * 容器中只存在一个实例
 */
public class SingletonScope implements Scope {

    private final ConcurrentHashMap<String, Object> instances = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "singleton";
    }

    @Override
    public Object get(String beanName, BeanContainer beanFactory, BeanCreator beanCreator) {
        return instances.computeIfAbsent(beanName, name -> {
            try {
                return beanCreator.create();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton bean: " + name, e);
            }
        });
    }

    /**
     * 销毁所有单例Bean
     */
    public void destroy() {
        instances.clear();
    }

    /**
     * 获取所有单例实例
     */
    public ConcurrentHashMap<String, Object> getInstances() {
        return instances;
    }
}
