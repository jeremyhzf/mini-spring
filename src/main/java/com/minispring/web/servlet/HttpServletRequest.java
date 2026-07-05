package com.minispring.web.servlet;

import java.util.Map;

/**
 * HTTP请求接口
 */
public interface HttpServletRequest {

    /**
     * 获取请求方法
     */
    String getMethod();

    /**
     * 获取请求URI
     */
    String getRequestURI();

    /**
     * 获取请求参数
     */
    String getParameter(String name);

    /**
     * 获取所有请求参数
     */
    Map<String, String[]> getParameterMap();

    /**
     * 设置请求属性
     */
    void setAttribute(String name, Object value);

    /**
     * 获取请求属性
     */
    Object getAttribute(String name);
}
