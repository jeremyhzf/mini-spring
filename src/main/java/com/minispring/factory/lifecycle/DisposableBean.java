package com.minispring.factory.lifecycle;

/**
 * Bean销毁接口
 * Bean实现此接口后，在容器关闭时会调用destroy方法
 */
public interface DisposableBean {

    /**
     * 在Bean销毁时调用
     * 用于执行清理逻辑
     */
    void destroy();
}
