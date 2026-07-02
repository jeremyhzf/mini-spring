package com.minispring.test;

/**
 * 带依赖的服务（用于测试构造器注入）
 */
public class ServiceWithDependency {
    private final Repository repository;

    public ServiceWithDependency(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }
}
