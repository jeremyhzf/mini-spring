package com.minispring.factory.instantiator;

/**
 * 多Setter服务 - 用于测试
 */
class MultiSetterService {
    private Repository repo1;
    private Repository repo2;

    public void setRepo1(Repository repo1) {
        this.repo1 = repo1;
    }

    public void setRepo2(Repository repo2) {
        this.repo2 = repo2;
    }

    public Repository getRepo1() {
        return repo1;
    }

    public Repository getRepo2() {
        return repo2;
    }
}
