package com.minispring.env;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentTest {

    @Test
    void shouldResolveProperty() {
        Environment env = new StandardEnvironment();

        // 设置属性
        env.setProperty("app.name", "MiniSpring");

        assertEquals("MiniSpring", env.getProperty("app.name"));
        assertEquals("MiniSpring", env.resolvePlaceholders("${app.name}"));
    }

    @Test
    void shouldSupportDefaultValue() {
        Environment env = new StandardEnvironment();
        assertEquals("default", env.resolvePlaceholders("${missing.property:default}"));
    }
}
