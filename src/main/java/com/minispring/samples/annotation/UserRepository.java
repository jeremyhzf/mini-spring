package com.minispring.samples.annotation;

import com.minispring.stereotype.Repository;

@Repository
public class UserRepository {
    public void save(String username) {
        System.out.println("   保存用户: " + username);
    }
}
