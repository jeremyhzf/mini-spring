package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.InitializingBean;
import com.minispring.factory.lifecycle.DisposableBean;
import com.minispring.samples.repository.UserRepository;

public class UserService implements InitializingBean, DisposableBean {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("1. UserService构造器调用");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("2. UserService初始化回调执行");
        System.out.println("   依赖检查: userRepository = " + (userRepository != null ? "已注入" : "未注入"));
    }

    public void createUser(String username) {
        System.out.println("   创建用户: " + username);
        userRepository.save(username);
    }

    @Override
    public void destroy() {
        System.out.println("3. UserService销毁回调执行");
    }
}
