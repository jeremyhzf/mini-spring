package com.minispring.factory.scope;

import java.util.HashMap;
import java.util.Map;

/**
 * 作用域注册表
 * 管理所有作用域实例
 */
public class ScopeRegistry {

    private final Map<String, Scope> scopes = new HashMap<>();

    public ScopeRegistry() {
        // 注册默认作用域
        registerScope("singleton", new SingletonScope());
        registerScope("prototype", new PrototypeScope());
    }

    /**
     * 注册作用域
     */
    public void registerScope(String name, Scope scope) {
        scopes.put(name, scope);
    }

    /**
     * 获取作用域
     */
    public Scope getScope(String name) {
        Scope scope = scopes.get(name);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown scope: " + name);
        }
        return scope;
    }
}
