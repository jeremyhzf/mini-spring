package com.minispring.web.controller;

import com.minispring.web.servlet.HttpServletRequest;
import com.minispring.web.servlet.HttpServletResponse;
import com.minispring.web.ModelAndView;

/**
 * 控制器接口
 */
@FunctionalInterface
public interface Controller {

    /**
     * 处理请求
     *
     * @return ModelAndView 包含模型和视图信息
     */
    ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
