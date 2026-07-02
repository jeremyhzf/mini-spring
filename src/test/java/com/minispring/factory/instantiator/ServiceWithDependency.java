package com.minispring.factory.instantiator;

public class ServiceWithDependency {
    private final Repository repository;

    public ServiceWithDependency(Repository repository) {
        this.repository = repository;
    }
}
