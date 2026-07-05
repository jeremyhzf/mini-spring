package com.minispring.web.servlet;

import com.minispring.web.annotation.GetMapping;
import com.minispring.web.annotation.PostMapping;
import com.minispring.web.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于注解的处理器映射
 */
public class RequestMappingHandlerMapping implements HandlerMapping {

    private Map<String, HandlerMethod> handlerMethods = new HashMap<>();

    /**
     * 注册处理器
     */
    public void registerHandler(Object handler) {
        Class<?> handlerClass = handler.getClass();

        // 检查类级别的@RequestMapping
        String classPath = "";
        RequestMapping classMapping = handlerClass.getDeclaredAnnotation(RequestMapping.class);
        if (classMapping != null) {
            classPath = classMapping.value();
        }

        // 遍历所有方法
        for (Method method : handlerClass.getDeclaredMethods()) {
            // 检查方法级别的映射注解
            RequestMapping requestMapping = method.getDeclaredAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String path = classPath + requestMapping.value();
                // 如果有指定HTTP方法，使用方法前缀
                RequestMapping.RequestMethod[] httpMethods = requestMapping.method();
                if (httpMethods.length > 0) {
                    for (RequestMapping.RequestMethod httpMethod : httpMethods) {
                        handlerMethods.put(httpMethod.name() + ":" + path, new HandlerMethod(handler, method));
                    }
                } else {
                    handlerMethods.put(path, new HandlerMethod(handler, method));
                }
            }

            GetMapping getMapping = method.getDeclaredAnnotation(GetMapping.class);
            if (getMapping != null) {
                String path = classPath + getMapping.value();
                handlerMethods.put("GET:" + path, new HandlerMethod(handler, method));
            }

            PostMapping postMapping = method.getDeclaredAnnotation(PostMapping.class);
            if (postMapping != null) {
                String path = classPath + postMapping.value();
                handlerMethods.put("POST:" + path, new HandlerMethod(handler, method));
            }
        }
    }

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String key = method + ":" + uri;
        HandlerMethod handlerMethod = handlerMethods.get(key);

        if (handlerMethod == null) {
            // 尝试不带方法前缀匹配
            handlerMethod = handlerMethods.get(uri);
        }

        if (handlerMethod != null) {
            return new HandlerExecutionChain(handlerMethod);
        }

        return null;
    }

    /**
     * 处理器方法封装
     */
    public static class HandlerMethod {
        private final Object handler;
        private final Method method;

        public HandlerMethod(Object handler, Method method) {
            this.handler = handler;
            this.method = method;
            this.method.setAccessible(true);
        }

        public Object getHandler() {
            return handler;
        }

        public Method getMethod() {
            return method;
        }

        public Object invoke(Object... args) throws Exception {
            return method.invoke(handler, args);
        }
    }
}
