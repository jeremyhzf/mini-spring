package com.minispring.samples.service;

import com.minispring.samples.repository.UserRepository;

/**
 * 用户服务类
 */
public class UserService {

    private UserRepository userRepository;

    // 通过Setter注入（目前阶段2才实现，这里先手动设置）
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(String username) {
        System.out.println("创建用户: " + username);
        if (userRepository != null) {
            userRepository.save(username);
        }
    }
}
