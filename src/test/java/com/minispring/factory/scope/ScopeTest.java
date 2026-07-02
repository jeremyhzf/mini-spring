package com.minispring.factory.scope;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {

    @Test
    void shouldReturnSameInstanceForSingleton() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("singleton", SingletonBean.class);

        Object bean1 = container.getBean("singleton");
        Object bean2 = container.getBean("singleton");

        assertSame(bean1, bean2);
    }

    @Test
    void shouldReturnDifferentInstanceForPrototype() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("prototype", PrototypeBean.class);

        Object bean1 = container.getBean("prototype");
        Object bean2 = container.getBean("prototype");

        assertNotSame(bean1, bean2);
    }
}
