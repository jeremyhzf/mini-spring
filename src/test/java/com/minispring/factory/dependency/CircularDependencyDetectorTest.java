package com.minispring.factory.dependency;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.factory.dependency.CircularDependencyDetector.CircularDependencyException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CircularDependencyDetectorTest {

    @Test
    void shouldDetectCircularDependency() {
        BeanContainer container = new DefaultBeanContainer();

        container.registerBean("serviceA", ServiceA.class);
        container.registerBean("serviceB", ServiceB.class);

        assertThrows(CircularDependencyException.class, () -> {
            container.getBean("serviceA");
        });
    }
}
