package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 作用域注解测试
 */
public class AnnotationScopeTest {

    @Test
    void shouldRespectScopeAnnotation() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("annotatedPrototype", AnnotatedPrototypeBean.class);

        Object bean1 = container.getBean("annotatedPrototype");
        Object bean2 = container.getBean("annotatedPrototype");

        assertNotSame(bean1, bean2);
    }

    @Test
    void shouldRespectSingletonAnnotation() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("annotatedSingleton", AnnotatedSingletonBean.class);

        Object bean1 = container.getBean("annotatedSingleton");
        Object bean2 = container.getBean("annotatedSingleton");

        assertSame(bean1, bean2);
    }
}
