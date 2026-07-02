package com.minispring.factory.instantiator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SetterInjectorTest {

    @Test
    void shouldInjectViaSetter() {
        SetterInjector injector = new SetterInjector();
        ServiceWithSetter service = new ServiceWithSetter();
        Repository repository = new RepositoryImpl();

        injector.inject(service, "setRepository", repository);

        assertNotNull(service.getRepository());
        assertTrue(service.getRepository() instanceof RepositoryImpl);
    }

    @Test
    void shouldHandleMultipleSetters() {
        SetterInjector injector = new SetterInjector();
        MultiSetterService service = new MultiSetterService();
        Repository repo1 = new RepositoryImpl();
        Repository repo2 = new RepositoryImpl();

        injector.inject(service, "setRepo1", repo1);
        injector.inject(service, "setRepo2", repo2);

        assertNotNull(service.getRepo1());
        assertNotNull(service.getRepo2());
    }
}
