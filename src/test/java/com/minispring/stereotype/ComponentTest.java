package com.minispring.stereotype;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ComponentTest {

    @Test
    void shouldDefineComponentAnnotation() {
        assertNotNull(TestComponent.class.getAnnotation(Component.class));
    }

    @Test
    void shouldDefineRepositoryAnnotation() {
        assertNotNull(TestRepository.class.getAnnotation(Repository.class));
    }

    @Test
    void shouldDefineServiceAnnotation() {
        assertNotNull(TestService.class.getAnnotation(Service.class));
    }

    @Test
    void shouldDefineControllerAnnotation() {
        assertNotNull(TestController.class.getAnnotation(Controller.class));
    }

    @Component
    static class TestComponent {}

    @Repository
    static class TestRepository {}

    @Service
    static class TestService {}

    @Controller
    static class TestController {}
}
