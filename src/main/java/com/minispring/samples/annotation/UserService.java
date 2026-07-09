package com.minispring.samples.annotation;

import com.minispring.annotation.Autowired;
import com.minispring.annotation.Value;
import com.minispring.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Value("${app.name:mini-spring}")
    private String appName;

    public void createUser(String username) {
        System.out.println("   [应用: " + appName + "] 创建用户: " + username);
        userRepository.save(username);
    }
}
