package com.minispring.web.samples;

import com.minispring.web.servlet.DispatcherServlet;

/**
 * Web应用启动类
 */
public class WebApplication {

    public static void main(String[] args) throws Exception {
        // 创建DispatcherServlet
        DispatcherServlet servlet = new DispatcherServlet();

        // 注册控制器
        servlet.registerController(new UserController());

        System.out.println("Web应用已启动，控制器已注册:");
        System.out.println(" - GET /user/list - 列出所有用户");
        System.out.println(" - GET /user/detail?id={id} - 获取用户详情");
        System.out.println(" - POST /user/create?name={name} - 创建新用户");
    }
}
