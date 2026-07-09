package com.minispring.web.servlet;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器执行链
 */
public class HandlerExecutionChain {

    private final Object handler;
    private final List<HandlerInterceptor> interceptors = new ArrayList<>();

    public HandlerExecutionChain(Object handler) {
        this.handler = handler;
    }

    /**
     * 获取处理器
     */
    public Object getHandler() {
        return handler;
    }

    /**
     * 添加拦截器
     */
    public void addInterceptor(HandlerInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    /**
     * 获取所有拦截器
     */
    public List<HandlerInterceptor> getInterceptors() {
        return interceptors;
    }
}
