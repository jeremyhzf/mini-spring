package com.minispring.samples.repository;

/**
 * 用户仓储接口
 */
public class UserRepository {

    public void save(String username) {
        System.out.println("保存用户: " + username);
    }

    public String find(String username) {
        return "用户: " + username;
    }
}
