package com.minispring.web.servlet;

/**
 * 处理器拦截器接口
 */
public interface HandlerInterceptor {

    /**
     * 前置处理
     *
     * @return true表示继续执行，false表示中断
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    /**
     * 后置处理
     */
    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    /**
     * 完成后处理
     */
    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception;
}
