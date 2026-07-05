package com.minispring.web.view;

import com.minispring.web.servlet.HttpServletResponse;

import java.io.PrintWriter;
import java.util.Map;

/**
 * 内部资源视图
 * 简化实现，直接渲染HTML
 */
public class InternalResourceView implements View {

    private String url;

    public InternalResourceView(String url) {
        this.url = url;
    }

    @Override
    public void render(Map<String, Object> model, Object response) throws Exception {
        if (!(response instanceof HttpServletResponse)) {
            throw new IllegalArgumentException("Response must be HttpServletResponse");
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("text/html");
        PrintWriter writer = httpResponse.getWriter();

        writer.println("<html><body>");
        writer.println("<h1>View: " + url + "</h1>");

        if (model != null && !model.isEmpty()) {
            writer.println("<h2>Model:</h2>");
            writer.println("<ul>");
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                writer.println("<li>" + entry.getKey() + ": " + entry.getValue() + "</li>");
            }
            writer.println("</ul>");
        }

        writer.println("</body></html>");
    }
}
