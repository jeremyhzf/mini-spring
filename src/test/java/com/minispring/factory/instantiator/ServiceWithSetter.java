package com.minispring.factory.instantiator;

/**
 * 带Setter的服务 - 用于测试
 */
class ServiceWithSetter {
    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        return repository;
    }
}
