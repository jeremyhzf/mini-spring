package com.minispring.web.view;

import com.minispring.web.servlet.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ViewResolver测试
 */
class ViewResolverTest {

    private InternalResourceViewResolver viewResolver;
    private HttpServletResponse mockResponse;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp() {
        viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        stringWriter = new StringWriter();
        mockResponse = new HttpServletResponse() {
            private String contentType;
            private PrintWriter writer = new PrintWriter(stringWriter);

            @Override
            public void setContentType(String type) {
                this.contentType = type;
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                return writer;
            }
        };
    }

    @Test
    void testResolveViewName() throws Exception {
        View view = viewResolver.resolveViewName("home");
        assertNotNull(view);
        assertTrue(view instanceof InternalResourceView);
    }

    @Test
    void testResolveViewNameWithPrefixAndSuffix() throws Exception {
        InternalResourceView view = (InternalResourceView) viewResolver.resolveViewName("home");

        // 验证视图渲染时包含了正确的URL
        Map<String, Object> model = new HashMap<>();
        model.put("message", "Hello World");
        view.render(model, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("/WEB-INF/views/home.jsp"));
    }

    @Test
    void testRenderWithModel() throws Exception {
        View view = viewResolver.resolveViewName("user");

        Map<String, Object> model = new HashMap<>();
        model.put("name", "John");
        model.put("age", 30);
        model.put("email", "john@example.com");

        view.render(model, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("name: John"));
        assertTrue(output.contains("age: 30"));
        assertTrue(output.contains("email: john@example.com"));
    }

    @Test
    void testRenderWithEmptyModel() throws Exception {
        View view = viewResolver.resolveViewName("empty");
        view.render(new HashMap<>(), mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("<html><body>"));
        assertTrue(output.contains("</body></html>"));
        assertFalse(output.contains("Model:"));
    }

    @Test
    void testRenderWithNullModel() throws Exception {
        View view = viewResolver.resolveViewName("nullModel");
        view.render(null, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("<html><body>"));
        assertFalse(output.contains("Model:"));
    }

    @Test
    void testInternalResourceView() throws Exception {
        InternalResourceView view = new InternalResourceView("/test/page.html");

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Test Page");

        view.render(model, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("/test/page.html"));
        assertTrue(output.contains("title: Test Page"));
    }

    @Test
    void testInvalidResponseType() throws Exception {
        View view = viewResolver.resolveViewName("test");
        Map<String, Object> model = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> {
            view.render(model, "not a response");
        });
    }
}
