package com.minispring.samples.annotation;

import com.minispring.annotation.Autowired;
import com.minispring.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void createUser(String username) {
        System.out.println("   创建用户: " + username);
        userRepository.save(username);
    }
}
