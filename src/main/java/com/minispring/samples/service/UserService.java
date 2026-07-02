package com.minispring.samples.service;

import com.minispring.samples.repository.UserRepository;

/**
 * 用户服务类 - 支持构造器注入
 */
public class UserService {

    private final UserRepository userRepository;

    // 构造器注入
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("UserService创建完成，依赖已注入: " + userRepository.getClass().getSimpleName());
    }

    public void createUser(String username) {
        System.out.println("创建用户: " + username);
        if (userRepository != null) {
            userRepository.save(username);
        }
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }
}
