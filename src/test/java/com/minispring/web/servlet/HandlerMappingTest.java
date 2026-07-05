package com.minispring.web.servlet;

import com.minispring.web.annotation.GetMapping;
import com.minispring.web.annotation.PostMapping;
import com.minispring.web.annotation.RequestMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HandlerMapping测试
 */
class HandlerMappingTest {

    private RequestMappingHandlerMapping handlerMapping;
    private TestController testController;
    private ApiUserController apiUserController;

    @BeforeEach
    void setUp() {
        handlerMapping = new RequestMappingHandlerMapping();
        testController = new TestController();
        handlerMapping.registerHandler(testController);
        apiUserController = new ApiUserController();
        handlerMapping.registerHandler(apiUserController);
    }

    @Test
    void testGetMapping() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/hello");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, "HandlerExecutionChain should not be null");
        assertTrue(chain.getHandler() instanceof RequestMappingHandlerMapping.HandlerMethod,
                "Handler should be HandlerMethod");

        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();
        assertEquals("hello", handlerMethod.getMethod().getName(),
                "Method name should be 'hello'");
    }

    @Test
    void testPostMapping() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/create");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, "HandlerExecutionChain should not be null");
        assertTrue(chain.getHandler() instanceof RequestMappingHandlerMapping.HandlerMethod,
                "Handler should be HandlerMethod");

        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();
        assertEquals("create", handlerMethod.getMethod().getName(),
                "Method name should be 'create'");
    }

    @Test
    void testRequestMappingWithoutMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/info");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, "HandlerExecutionChain should not be null");
        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();
        assertEquals("info", handlerMethod.getMethod().getName(),
                "Method name should be 'info'");
    }

    @Test
    void testRequestMappingWithMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/test/update");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, "HandlerExecutionChain should not be null");
        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();
        assertEquals("update", handlerMethod.getMethod().getName(),
                "Method name should be 'update'");
    }

    @Test
    void testMethodNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/notfound");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNull(chain, "HandlerExecutionChain should be null for non-existent path");
    }

    @Test
    void testWrongHttpMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/hello");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNull(chain, "HandlerExecutionChain should be null for wrong HTTP method");
    }

    @Test
    void testClassLevelMapping() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, "HandlerExecutionChain should not be null");
        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();
        assertEquals("listUsers", handlerMethod.getMethod().getName(),
                "Method name should be 'listUsers'");
    }

    @Test
    void testHandlerMethodInvoke() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/hello");

        HandlerExecutionChain chain = handlerMapping.getHandler(request);
        RequestMappingHandlerMapping.HandlerMethod handlerMethod =
                (RequestMappingHandlerMapping.HandlerMethod) chain.getHandler();

        Object result = handlerMethod.invoke();

        assertEquals("Hello World", result, "Invoke result should be 'Hello World'");
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

        @RequestMapping("/info")
        public String info() {
            return "Info";
        }

        @RequestMapping(value = "/update", method = RequestMapping.RequestMethod.PUT)
        public String update() {
            return "Updated";
        }
    }

    /**
     * 带类级别映射的Controller
     */
    @RequestMapping("/api")
    public static class ApiUserController {

        @GetMapping("/users")
        public String listUsers() {
            return "Users";
        }
    }

    /**
     * Mock HttpServletRequest for testing
     */
    private static class MockHttpServletRequest implements HttpServletRequest {
        private final String method;
        private final String requestURI;

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
        public java.util.Map<String, String[]> getParameterMap() {
            return java.util.Collections.emptyMap();
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }
    }
}
