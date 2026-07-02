package com.minispring.factory.lifecycle;

public class LifecycleBean implements InitializingBean, DisposableBean {
    private boolean initialized = false;
    private boolean destroyed = false;
    private String status = "created";

    @Override
    public void afterPropertiesSet() {
        this.initialized = true;
        this.status = "init called";
    }

    @Override
    public void destroy() {
        this.destroyed = true;
        this.status = "destroyed";
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
