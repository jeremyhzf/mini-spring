package com.minispring.samples.mvc;

import com.minispring.web.ModelAndView;
import com.minispring.web.annotation.GetMapping;
import com.minispring.web.annotation.PostMapping;
import com.minispring.web.annotation.RequestMapping;
import com.minispring.web.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器示例
 */
@RequestMapping("/user")
public class UserController {

    // 模拟数据库（id -> name）
    private final Map<String, String> users = new HashMap<>();

    /**
     * 列出所有用户
     */
    @GetMapping("/list")
    public ModelAndView listUsers() {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("user/list");
        mav.addObject("users", users.values());
        return mav;
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail")
    public String getUserDetail(@RequestParam("id") String id) {
        return "User detail: " + users.get(id);
    }

    /**
     * 创建新用户
     */
    @PostMapping("/create")
    public String createUser(@RequestParam("name") String name) {
        String id = String.valueOf(users.size() + 1);
        users.put(id, name);
        return "User created: " + name + " (ID: " + id + ")";
    }
}
