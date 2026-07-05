package com.minispring.web;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型和视图容器
 */
public class ModelAndView {

    private Object view;
    private Map<String, Object> model = new HashMap<>();

    public ModelAndView() {}

    public ModelAndView(Object view) {
        this.view = view;
    }

    public ModelAndView(Object view, Map<String, Object> model) {
        this.view = view;
        this.model = model;
    }

    /**
     * 设置视图
     */
    public void setView(Object view) {
        this.view = view;
    }

    /**
     * 获取视图
     */
    public Object getView() {
        return view;
    }

    /**
     * 设置视图名称
     */
    public void setViewName(String viewName) {
        this.view = viewName;
    }

    /**
     * 获取视图名称
     */
    public String getViewName() {
        return view != null ? view.toString() : null;
    }

    /**
     * 添加模型数据
     */
    public void addObject(String key, Object value) {
        this.model.put(key, value);
    }

    /**
     * 获取所有模型数据
     */
    public Map<String, Object> getModel() {
        return model;
    }

    /**
     * 判断是否有视图
     */
    public boolean hasView() {
        return view != null;
    }
}
