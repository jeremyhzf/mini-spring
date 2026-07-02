package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DependencyResolverTest {

    @Test
    void shouldResolveSingleDependency() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("repository", Repository.class);

        DependencyResolver resolver = new DependencyResolver(container);
        Object dependency = resolver.resolve(Repository.class);

        assertNotNull(dependency);
        assertTrue(dependency instanceof Repository);
    }

    @Test
    void shouldResolveMultipleDependencies() {
        BeanContainer container = new DefaultBeanContainer();
        container.registerBean("repository1", Repository1.class);
        container.registerBean("repository2", Repository2.class);

        DependencyResolver resolver = new DependencyResolver(container);

        Object dep1 = resolver.resolve(Repository1.class);
        Object dep2 = resolver.resolve(Repository2.class);

        assertNotNull(dep1);
        assertNotNull(dep2);
    }
}
