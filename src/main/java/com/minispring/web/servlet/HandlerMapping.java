package com.minispring.web.servlet;

/**
 * 处理器映射接口
 * 根据请求找到对应的处理器
 */
public interface HandlerMapping {

    /**
     * 根据请求获取处理器执行链
     *
     * @param request HTTP请求
     * @return 处理器执行链，如果没有找到返回null
     */
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
