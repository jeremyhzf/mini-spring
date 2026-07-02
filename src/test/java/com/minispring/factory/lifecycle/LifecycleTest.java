package com.minispring.factory.lifecycle;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LifecycleTest {

    @Test
    void shouldCallInitializingBean() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("lifecycleBean", LifecycleBean.class);

        LifecycleBean bean = (LifecycleBean) container.getBean("lifecycleBean");

        assertTrue(bean.isInitialized());
        assertEquals("init called", bean.getStatus());
    }

    @Test
    void shouldCallBeanPostProcessorBeforeInitialization() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("processor", TestPostProcessor.class);
        container.registerBean("bean", LifecycleBean.class);

        LifecycleBean bean = (LifecycleBean) container.getBean("bean");

        assertEquals("post-processed", bean.getStatus());
    }
}
