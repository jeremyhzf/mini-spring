package com.minispring.web.view;

/**
 * 视图解析器接口
 * 负责将视图名称解析为View对象
 */
public interface ViewResolver {

    /**
     * 解析视图
     *
     * @param viewName 视图名称
     * @return View对象，如果无法解析返回null
     */
    View resolveViewName(String viewName) throws Exception;
}
