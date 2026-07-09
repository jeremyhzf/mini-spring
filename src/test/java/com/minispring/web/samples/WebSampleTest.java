package com.minispring.web.samples;

import com.minispring.samples.mvc.UserController;
import com.minispring.web.servlet.DispatcherServlet;
import com.minispring.web.servlet.MockHttpServletRequest;
import com.minispring.web.servlet.MockHttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebSampleTest {

    @Test
    void shouldHandleUserListRequest() throws Exception {
        DispatcherServlet servlet = new DispatcherServlet();
        servlet.registerController(new UserController());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.doGet(request, response);

        assertTrue(response.getContent().contains("user/list"));
    }

    @Test
    void shouldHandleUserDetailRequest() throws Exception {
        DispatcherServlet servlet = new DispatcherServlet();
        servlet.registerController(new UserController());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/detail");
        request.setParameter("id", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.doGet(request, response);

        assertTrue(response.getContent().contains("User detail"));
    }

    @Test
    void shouldHandleUserCreateRequest() throws Exception {
        DispatcherServlet servlet = new DispatcherServlet();
        servlet.registerController(new UserController());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/create");
        request.setParameter("name", "张三");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.doPost(request, response);

        assertTrue(response.getContent().contains("User created"));
    }
}
