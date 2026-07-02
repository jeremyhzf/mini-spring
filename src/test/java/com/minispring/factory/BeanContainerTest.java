package com.minispring.factory;

import com.minispring.test.BeanWithoutNoArgConstructor;
import com.minispring.test.Repository;
import com.minispring.test.RepositoryImpl;
import com.minispring.test.ServiceWithDependency;
import com.minispring.test.SimpleBean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeanContainerTest {

    @Test
    void shouldDefineBeanContainerInterface() {
        // 这个测试验证接口可以被正确编译和实例化
        BeanContainer container = new DefaultBeanContainer();

        assertNotNull(container);
        assertTrue(container instanceof BeanContainer);
    }

    @Test
    void shouldRegisterAndGetBean() {
        BeanContainer container = new DefaultBeanContainer();

        // 注册一个简单的Bean
        container.registerBean("testBean", SimpleBean.class);

        // 获取Bean
        Object bean = container.getBean("testBean");

        assertNotNull(bean);
        assertTrue(bean instanceof SimpleBean);
    }

    @Test
    void shouldReturnSameInstanceForSameBeanName() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("testBean", SimpleBean.class);

        Object bean1 = container.getBean("testBean");
        Object bean2 = container.getBean("testBean");

        assertSame(bean1, bean2, "应该返回同一个实例");
    }

    @Test
    void shouldThrowExceptionWhenBeanNotFound() {
        BeanContainer container = new DefaultBeanContainer();

        assertThrows(BeanNotFoundException.class, () -> {
            container.getBean("nonExistentBean");
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        BeanContainer container = new DefaultBeanContainer();

        assertThrows(IllegalArgumentException.class, () -> {
            container.registerBean(null, SimpleBean.class);
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        BeanContainer container = new DefaultBeanContainer();

        assertThrows(IllegalArgumentException.class, () -> {
            container.registerBean("", SimpleBean.class);
        });
    }

    @Test
    void shouldThrowExceptionWhenClassIsNull() {
        BeanContainer container = new DefaultBeanContainer();

        assertThrows(IllegalArgumentException.class, () -> {
            container.registerBean("testBean", null);
        });
    }

    @Test
    void shouldThrowExceptionWhenNoNoArgConstructor() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("noArgBean", BeanWithoutNoArgConstructor.class);

        assertThrows(RuntimeException.class, () -> {
            container.getBean("noArgBean");
        });
    }

    @Test
    void shouldInjectDependenciesViaConstructor() {
        BeanContainer container = new DefaultBeanContainer();

        // 注册依赖
        container.registerBean("repository", RepositoryImpl.class);
        // 注册需要依赖的Bean
        container.registerBean("service", ServiceWithDependency.class);

        // 获取Service，其依赖应该被自动注入
        ServiceWithDependency service = (ServiceWithDependency) container.getBean("service");

        assertNotNull(service);
        assertNotNull(service.getRepository());
        assertTrue(service.getRepository() instanceof RepositoryImpl);
    }

    @Test
    void shouldThrowExceptionWhenDependencyNotFound() {
        BeanContainer container = new DefaultBeanContainer();

        // 注册需要依赖的Bean，但不注册依赖
        container.registerBean("service", ServiceWithDependency.class);

        assertThrows(BeanNotFoundException.class, () -> {
            container.getBean("service");
        });
    }
}
