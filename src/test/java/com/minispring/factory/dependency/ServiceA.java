package com.minispring.factory.dependency;

// 循环依赖示例
class ServiceA {
    private final ServiceB serviceB;

    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }

    public ServiceB getServiceB() {
        return serviceB;
    }
}
