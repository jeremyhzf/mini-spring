package com.minispring.factory.dependency;

// 循环依赖示例
class ServiceB {
    private final ServiceA serviceA;

    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }

    public ServiceA getServiceA() {
        return serviceA;
    }
}
