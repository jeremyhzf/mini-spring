package com.minispring.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Mock HTTP响应实现
 * 用于测试目的
 */
public class MockHttpServletResponse implements HttpServletResponse {

    private String contentType;
    private final StringWriter stringWriter;
    private final PrintWriter printWriter;

    /**
     * 创建Mock响应
     */
    public MockHttpServletResponse() {
        this.stringWriter = new StringWriter();
        this.printWriter = new PrintWriter(stringWriter);
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    /**
     * 获取内容类型
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取响应内容
     *
     * @return 响应内容
     */
    public String getContent() {
        printWriter.flush();
        return stringWriter.toString();
    }
}
