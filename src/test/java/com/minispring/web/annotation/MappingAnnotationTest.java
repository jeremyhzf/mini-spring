package com.minispring.web.annotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class MappingAnnotationTest {

    @Test
    void shouldDefineRequestMapping() {
        RequestMapping mapping = TestController.class.getDeclaredAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/api", mapping.value());
    }

    @Test
    void shouldDefineGetMapping() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("getUser", String.class);
        GetMapping mapping = method.getDeclaredAnnotation(GetMapping.class);
        assertNotNull(mapping);
        assertEquals("/user/{id}", mapping.value());
    }

    @Test
    void shouldDefinePostMapping() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("createUser", String.class);
        PostMapping mapping = method.getDeclaredAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertEquals("/user", mapping.value());
    }

    @Test
    void shouldHaveRequestMethodOnGetMapping() {
        RequestMapping requestMapping = GetMapping.class.getDeclaredAnnotation(RequestMapping.class);

        assertNotNull(requestMapping);
        assertEquals(1, requestMapping.method().length);
        assertEquals(RequestMapping.RequestMethod.GET, requestMapping.method()[0]);
    }

    @Test
    void shouldHaveRequestMethodOnPostMapping() {
        RequestMapping requestMapping = PostMapping.class.getDeclaredAnnotation(RequestMapping.class);

        assertNotNull(requestMapping);
        assertEquals(1, requestMapping.method().length);
        assertEquals(RequestMapping.RequestMethod.POST, requestMapping.method()[0]);
    }

    @RequestMapping("/api")
    static class TestController {

        @GetMapping("/user/{id}")
        public String getUser(@RequestParam("id") String id) {
            return "user";
        }

        @PostMapping("/user")
        public String createUser(@RequestParam("name") String name) {
            return "created";
        }
    }
}
