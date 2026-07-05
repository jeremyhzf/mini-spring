package com.minispring.web.servlet;

/**
 * Web应用上下文接口
 *
 * <p>WebApplicationContext是Web应用的上下文抽象，提供了：</p>
 * <ul>
 *   <li>获取DispatcherServlet的入口</li>
 *   <li>初始化和刷新上下文的能力</li>
 *   <li>管理Web组件的生命周期</li>
 * </ul>
 *
 * @author mini-spring
 * @since 1.0.0
 */
public interface WebApplicationContext {

    /**
     * 获取DispatcherServlet
     *
     * @return DispatcherServlet实例
     */
    DispatcherServlet getDispatcherServlet();

    /**
     * 刷新上下文
     * 初始化所有组件，完成Web应用的启动准备
     *
     * @throws Exception 刷新过程中发生的异常
     */
    void refresh() throws Exception;

    /**
     * 关闭上下文
     * 清理资源，销毁所有单例Bean
     *
     * @throws Exception 关闭过程中发生的异常
     */
    void close() throws Exception;
}
