package com.minispring.web.servlet;

import com.minispring.factory.BeanContainer;
import com.minispring.factory.DefaultBeanContainer;
import com.minispring.web.ModelAndView;
import com.minispring.web.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * 前端控制器
 * 分发请求到对应的处理器
 *
 * <p>DispatcherServlet是MVC框架的核心，作为前端控制器负责：</p>
 * <ul>
 *   <li>接收所有HTTP请求</li>
 *   <li>通过HandlerMapping找到对应的处理器</li>
 *   <li>执行处理器并获取返回结果</li>
 *   <li>渲染视图或返回响应</li>
 * </ul>
 *
 * @author mini-spring
 * @since 1.0.0
 */
public class DispatcherServlet implements HttpServlet {

    private final List<HandlerMapping> handlerMappings = new ArrayList<>();
    private final BeanContainer beanContainer;

    /**
     * 默认构造函数
     * 创建DispatcherServlet并初始化Bean容器和默认的HandlerMapping
     */
    public DispatcherServlet() {
        this.beanContainer = new DefaultBeanContainer();
        // 注册默认的HandlerMapping
        this.handlerMappings.add(new RequestMappingHandlerMapping());
    }

    /**
     * 添加HandlerMapping
     *
     * @param handlerMapping 要添加的HandlerMapping
     */
    public void addHandlerMapping(HandlerMapping handlerMapping) {
        if (handlerMapping != null) {
            handlerMappings.add(handlerMapping);
        }
    }

    /**
     * 注册控制器
     * 将控制器注册到Bean容器，并注册到所有HandlerMapping中
     *
     * @param controller 控制器对象
     */
    public void registerController(Object controller) {
        String beanName = controller.getClass().getSimpleName();
        beanContainer.registerBean(beanName, controller.getClass());

        // 注册到所有HandlerMapping
        for (HandlerMapping handlerMapping : handlerMappings) {
            if (handlerMapping instanceof RequestMappingHandlerMapping) {
                Object handler = beanContainer.getBean(beanName);
                ((RequestMappingHandlerMapping) handlerMapping).registerHandler(handler);
            }
        }
    }

    /**
     * 获取Bean容器
     *
     * @return Bean容器实例
     */
    public BeanContainer getBeanContainer() {
        return beanContainer;
    }

    /**
     * 获取所有HandlerMapping
     *
     * @return HandlerMapping列表
     */
    public List<HandlerMapping> getHandlerMappings() {
        return new ArrayList<>(handlerMappings);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        processRequest(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        processRequest(request, response);
    }

    /**
     * 处理请求的核心方法
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @throws Exception 处理过程中发生的异常
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 1. 找到处理器
        HandlerExecutionChain handler = getHandler(request);

        if (handler == null) {
            sendNotFound(response);
            return;
        }

        // 2. 执行处理器
        Object result = executeHandler(handler, request, response);

        // 3. 处理结果
        handleResult(result, response);
    }

    /**
     * 获取处理器
     * 遍历所有HandlerMapping，找到能处理当前请求的处理器
     *
     * @param request HTTP请求
     * @return 处理器执行链，如果没有找到返回null
     * @throws Exception 获取处理器过程中发生的异常
     */
    private HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        for (HandlerMapping handlerMapping : handlerMappings) {
            HandlerExecutionChain handler = handlerMapping.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 执行处理器
     *
     * @param handler  处理器执行链
     * @param request  HTTP请求
     * @param response HTTP响应
     * @return 处理结果
     * @throws Exception 执行过程中发生的异常
     */
    private Object executeHandler(HandlerExecutionChain handler, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object handlerObject = handler.getHandler();

        if (handlerObject instanceof RequestMappingHandlerMapping.HandlerMethod handlerMethod) {
            Object[] args = resolveArguments(handlerMethod.getMethod(), request);
            return handlerMethod.invoke(args);
        }

        return handlerObject;
    }

    /**
     * 解析方法参数
     *
     * @param method  处理器方法
     * @param request HTTP请求
     * @return 参数数组
     */
    private Object[] resolveArguments(Method method, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);

            if (requestParam != null) {
                String paramName = requestParam.value();
                if (paramName.isEmpty()) {
                    paramName = parameter.getName();
                }
                args[i] = request.getParameter(paramName);
            } else {
                args[i] = null;
            }
        }

        return args;
    }

    /**
     * 处理处理器返回的结果
     *
     * @param result   处理结果
     * @param response HTTP响应
     * {@code @throwsException} 处理结果过程中发生的异常
     */
    private void handleResult(Object result, HttpServletResponse response) throws Exception {
        if (result == null) {
            response.setContentType("text/plain");
            response.getWriter().write("No content");
            return;
        }

        if (result instanceof ModelAndView) {
            // 渲染ModelAndView
            renderView((ModelAndView) result, response);
        } else if (result instanceof String) {
            // 返回字符串
            response.setContentType("text/plain");
            response.getWriter().write((String) result);
        } else {
            // 返回其他类型对象
            response.setContentType("application/json");
            response.getWriter().write(String.valueOf(result));
        }
    }

    /**
     * 渲染视图
     *
     * @param mav      ModelAndView对象
     * @param response HTTP响应
     * @throws Exception 渲染过程中发生的异常
     */
    private void renderView(ModelAndView mav, HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        response.getWriter().write("<html><body>");
        response.getWriter().write("<h1>View: " + mav.getViewName() + "</h1>");
        response.getWriter().write("<ul>");

        for (String key : mav.getModel().keySet()) {
            Object value = mav.getModel().get(key);
            response.getWriter().write("<li>" + key + ": " + value + "</li>");
        }

        response.getWriter().write("</ul>");
        response.getWriter().write("</body></html>");
    }

    /**
     * 发送404响应
     *
     * @param response HTTP响应
     * @throws Exception 发送过程中发生的异常
     */
    private void sendNotFound(HttpServletResponse response) throws Exception {
        response.setContentType("text/plain");
        response.getWriter().write("404 - Not Found");
    }
}
