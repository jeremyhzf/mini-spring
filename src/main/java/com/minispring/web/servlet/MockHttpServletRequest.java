package com.minispring.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock HTTP请求实现
 * 用于测试目的
 */
public class MockHttpServletRequest implements HttpServletRequest {

    private final String method;
    private final String requestURI;
    private final Map<String, String> parameters;
    private final Map<String, Object> attributes;

    /**
     * 创建Mock请求
     *
     * @param method     HTTP方法
     * @param requestURI 请求URI
     */
    public MockHttpServletRequest(String method, String requestURI) {
        this.method = method;
        this.requestURI = requestURI;
        this.parameters = new HashMap<>();
        this.attributes = new HashMap<>();
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
        return parameters.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            result.put(entry.getKey(), new String[]{entry.getValue()});
        }
        return result;
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * 设置请求参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }
}
