package com.minispring.samples.mvc;

import com.minispring.web.servlet.DispatcherServlet;

/**
 * 阶段6 - MVC 前端控制器示例
 * 演示：DispatcherServlet 注册控制器、请求映射、参数解析、返回值处理
 */
public class MvcDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 阶段6：MVC 前端控制器示例 ===\n");

        DispatcherServlet servlet = new DispatcherServlet();
        servlet.registerController(new UserController());

        // 先创建一个用户
        System.out.println("--- POST /user/create ---");
        dispatch(servlet, "POST", "/user/create", "name", "张三");

        // 列出用户
        System.out.println("\n--- GET /user/list ---");
        dispatch(servlet, "GET", "/user/list", null, null);

        // 查询用户详情
        System.out.println("\n--- GET /user/detail ---");
        dispatch(servlet, "GET", "/user/detail", "id", "1");

        System.out.println("\n=== 阶段6 示例结束 ===");
    }

    /**
     * 构造 Mock 请求并交由 DispatcherServlet 处理，打印响应内容
     */
    private static void dispatch(DispatcherServlet servlet, String method, String path,
                                 String paramKey, String paramValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (paramKey != null) {
            request.setParameter(paramKey, paramValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        if ("GET".equals(method)) {
            servlet.doGet(request, response);
        } else {
            servlet.doPost(request, response);
        }

        System.out.println("响应: " + response.getContent());
    }
}
