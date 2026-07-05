package com.minispring.web.servlet;

/**
 * HTTP Servlet接口
 * 简化的Servlet API
 */
public interface HttpServlet {

    /**
     * 处理GET请求
     */
    void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * 处理POST请求
     */
    void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
