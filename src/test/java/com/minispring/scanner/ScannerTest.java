package com.minispring.scanner;

import com.minispring.scanner.test.MyComponent;
import com.minispring.scanner.test.TestComponent;
import com.minispring.scanner.test.TestRepository;
import com.minispring.scanner.test.TestService;
import com.minispring.stereotype.Component;
import com.minispring.stereotype.Repository;
import com.minispring.stereotype.Service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

public class ScannerTest {

    @Test
    void shouldScanComponents() {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner("com.minispring.scanner.test");

        Set<Class<?>> components = scanner.scan();

        assertTrue(components.contains(TestComponent.class));
        assertTrue(components.contains(TestService.class));
        assertTrue(components.contains(TestRepository.class));
    }

    @Test
    void shouldGenerateBeanName() {
        ClassPathBeanScanner scanner = new ClassPathBeanScanner();

        assertEquals("testComponent", scanner.generateBeanName(TestComponent.class));
        assertEquals("myComponent", scanner.generateBeanName(MyComponent.class));
    }
}
