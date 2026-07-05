package com.minispring.web.view;

/**
 * 内部资源视图解析器
 * 将视图名称解析为InternalResourceView
 */
public class InternalResourceViewResolver implements ViewResolver {

    private String prefix = "";
    private String suffix = "";

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public View resolveViewName(String viewName) throws Exception {
        String url = prefix + viewName + suffix;
        return new InternalResourceView(url);
    }
}
