package com.minispring.web.servlet;

/**
 * HTTP响应接口
 */
public interface HttpServletResponse {

    /**
     * 设置内容类型
     */
    void setContentType(String type);

    /**
     * 获取写入器
     */
    java.io.PrintWriter getWriter() throws java.io.IOException;
}
