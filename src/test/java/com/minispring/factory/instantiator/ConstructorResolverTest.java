package com.minispring.factory.instantiator;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

public class ConstructorResolverTest {

    @Test
    void shouldResolveNoArgsConstructor() {
        ConstructorResolver resolver = new ConstructorResolver();
        Constructor<?> constructor = resolver.resolve(SimpleBean.class);

        assertNotNull(constructor);
        assertEquals(0, constructor.getParameterCount());
    }

    @Test
    void shouldResolveConstructorWithDependencies() {
        ConstructorResolver resolver = new ConstructorResolver();
        Constructor<?> constructor = resolver.resolve(ServiceWithDependency.class);

        assertNotNull(constructor);
        assertEquals(1, constructor.getParameterCount());
        assertEquals(Repository.class, constructor.getParameterTypes()[0]);
    }
}
