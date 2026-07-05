package com.minispring.web.servlet;

import com.minispring.web.ModelAndView;
import com.minispring.web.annotation.GetMapping;
import com.minispring.web.annotation.PostMapping;
import com.minispring.web.annotation.RequestMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DispatcherServlet测试
 */
class DispatcherServletTest {

    private DispatcherServlet dispatcherServlet;
    private TestController testController;

    @BeforeEach
    void setUp() {
        dispatcherServlet = new DispatcherServlet();
        testController = new TestController();
        dispatcherServlet.registerController(testController);
        // 注册带类级别映射的Controller
        ApiUserController apiUserController = new ApiUserController();
        dispatcherServlet.registerController(apiUserController);
    }

    @Test
    void testRegisterController() throws Exception {
        assertNotNull(dispatcherServlet.getBeanContainer().getBean("TestController"),
                "Controller should be registered in bean container");
    }

    @Test
    void testGetMappingWithStringResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        assertEquals("Hello World", response.getContent(),
                "Response should be 'Hello World'");
    }

    @Test
    void testPostMappingWithStringResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/create");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doPost(request, response);

        assertEquals("Created", response.getContent(),
                "Response should be 'Created'");
    }

    @Test
    void testGetMappingWithModelAndView() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/model");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        String content = response.getContent();
        assertTrue(content.contains("<h1>View: model</h1>"),
                "Response should contain view name");
        assertTrue(content.contains("<li>name: Test</li>"),
                "Response should contain model data");
    }

    @Test
    void testGetMappingWithObjectResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/data");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        assertEquals("application/json", response.getContentType(),
                "Content type should be application/json");
        assertEquals("42", response.getContent(),
                "Response should be '42'");
    }

    @Test
    void testNotFoundRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/notfound");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        assertEquals("404 - Not Found", response.getContent(),
                "Response should be '404 - Not Found'");
    }

    @Test
    void testWrongHttpMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/hello");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doPost(request, response);

        assertEquals("404 - Not Found", response.getContent(),
                "Response should be '404 - Not Found'");
    }

    @Test
    void testClassLevelMapping() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        assertEquals("Users List", response.getContent(),
                "Response should be 'Users List'");
    }

    @Test
    void testNullResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/null");
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcherServlet.doGet(request, response);

        assertEquals("No content", response.getContent(),
                "Response should be 'No content'");
    }

    @Test
    void testAddHandlerMapping() throws Exception {
        CustomHandlerMapping customMapping = new CustomHandlerMapping();
        dispatcherServlet.addHandlerMapping(customMapping);

        assertTrue(dispatcherServlet.getHandlerMappings().contains(customMapping),
                "Custom handler mapping should be added");
    }

    @Test
    void testGetBeanContainer() {
        assertNotNull(dispatcherServlet.getBeanContainer(),
                "Bean container should not be null");
    }

    /**
     * 测试用Controller
     */
    @RequestMapping("/test")
    public static class TestController {

        @GetMapping("/hello")
        public String hello() {
            return "Hello World";
        }

        @PostMapping("/create")
        public String create() {
            return "Created";
        }

        @GetMapping("/model")
        public ModelAndView getModel() {
            ModelAndView mav = new ModelAndView("model");
            mav.addObject("name", "Test");
            mav.addObject("value", 100);
            return mav;
        }

        @GetMapping("/data")
        public Integer getData() {
            return 42;
        }

        @GetMapping("/null")
        public Object getNull() {
            return null;
        }
    }

    /**
     * 带类级别映射的Controller
     */
    @RequestMapping("/api")
    public static class ApiUserController {

        @GetMapping("/users")
        public String listUsers() {
            return "Users List";
        }
    }

    /**
     * 自定义HandlerMapping
     */
    private static class CustomHandlerMapping implements HandlerMapping {

        @Override
        public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
            return null;
        }
    }

    /**
     * Mock HttpServletRequest for testing
     */
    private static class MockHttpServletRequest implements HttpServletRequest {
        private final String method;
        private final String requestURI;
        private final Map<String, Object> attributes = new HashMap<>();

        public MockHttpServletRequest(String method, String requestURI) {
            this.method = method;
            this.requestURI = requestURI;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getRequestURI() {
            return requestURI;
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return new HashMap<>();
        }

        @Override
        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }
    }

    /**
     * Mock HttpServletResponse for testing
     */
    private static class MockHttpServletResponse implements HttpServletResponse {
        private final StringWriter writer = new StringWriter();
        private String contentType;

        @Override
        public void setContentType(String type) {
            this.contentType = type;
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(writer);
        }

        public String getContent() {
            return writer.toString();
        }

        public String getContentType() {
            return contentType;
        }
    }
}
