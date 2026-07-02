package com.minispring.scanner;

import com.minispring.stereotype.Component;
import com.minispring.stereotype.Repository;
import com.minispring.stereotype.Service;
import com.minispring.stereotype.Controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类路径Bean扫描器
 * 扫描带有组件注解的类
 */
public class ClassPathBeanScanner {

    private final String basePackage;
    private final ClassPathScanner scanner;

    public ClassPathBeanScanner(String basePackage) {
        this.basePackage = basePackage;
        this.scanner = new ClassPathScanner(basePackage);
    }

    public ClassPathBeanScanner() {
        this("");
    }

    /**
     * 扫描所有带组件注解的类
     */
    public Set<Class<?>> scan() {
        Set<Class<?>> components = new HashSet<>();
        List<Class<?>> classes = scanner.scan();

        for (Class<?> clazz : classes) {
            if (isComponent(clazz)) {
                components.add(clazz);
            }
        }

        return components;
    }

    /**
     * 检查类是否是组件
     */
    private boolean isComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class) ||
               clazz.isAnnotationPresent(Repository.class) ||
               clazz.isAnnotationPresent(Service.class) ||
               clazz.isAnnotationPresent(Controller.class);
    }

    /**
     * 生成Bean名称
     */
    public String generateBeanName(Class<?> clazz) {
        // 尝试从注解获取名称
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }

        Service service = clazz.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }

        Repository repository = clazz.getAnnotation(Repository.class);
        if (repository != null && !repository.value().isEmpty()) {
            return repository.value();
        }

        Controller controller = clazz.getAnnotation(Controller.class);
        if (controller != null && !controller.value().isEmpty()) {
            return controller.value();
        }

        // 默认使用类名首字母小写
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
