package com.minispring.web.view;

import com.minispring.web.ModelAndView;
import java.util.Map;

/**
 * 视图接口
 */
public interface View {

    /**
     * 渲染视图
     *
     * @param model 模型数据
     * @param response HTTP响应
     */
    void render(Map<String, Object> model, Object response) throws Exception;
}
