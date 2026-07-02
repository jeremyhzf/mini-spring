package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.BeanNotFoundException;

/**
 * 依赖解析器
 * 根据类型从容器中解析依赖
 */
public class DependencyResolver {

    private final BeanContainer container;

    public DependencyResolver(BeanContainer container) {
        this.container = container;
    }

    /**
     * 根据类型解析依赖
     *
     * @param type 依赖的类型
     * @return 依赖实例
     * @throws BeanNotFoundException 如果依赖找不到
     */
    public Object resolve(Class<?> type) {
        // 简单实现：遍历所有Bean，按类型匹配
        // 后续会优化为按类型索引
        return resolveByType(type);
    }

    /**
     * 按类型解析Bean
     *
     * @param type 目标类型
     * @return 匹配的Bean实例
     */
    private Object resolveByType(Class<?> type) {
        // 如果容器是DefaultBeanContainer，可以获取Bean定义信息
        if (container instanceof com.minispring.factory.DefaultBeanContainer) {
            com.minispring.factory.DefaultBeanContainer defaultContainer =
                (com.minispring.factory.DefaultBeanContainer) container;

            // 尝试按类型名称查找
            String beanName = generateBeanName(type);
            try {
                return container.getBean(beanName);
            } catch (BeanNotFoundException e) {
                // 如果按名称找不到，尝试按类型查找所有Bean定义
                java.util.Map<String, Class<?>> beanDefinitions = defaultContainer.getBeanDefinitions();
                for (java.util.Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
                    if (type.isAssignableFrom(entry.getValue())) {
                        return container.getBean(entry.getKey());
                    }
                }
                throw new BeanNotFoundException("No bean found for type: " + type.getName());
            }
        }

        // 尝试按类型名称查找
        String beanName = generateBeanName(type);
        try {
            return container.getBean(beanName);
        } catch (BeanNotFoundException e) {
            // 如果找不到，抛出异常
            throw new BeanNotFoundException("No bean found for type: " + type.getName());
        }
    }

    /**
     * 根据类型生成Bean名称
     * 将类名首字母小写作为Bean名称
     */
    private String generateBeanName(Class<?> type) {
        String simpleName = type.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
