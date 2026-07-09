package com.minispring.event;

/**
 * 监听器异常处理器
 * 设置到多播器后，监听器抛出的异常交由本接口处理，分发继续后续监听器。
 * 未设置时，异常向外传播并中断分发。
 */
public interface ErrorHandler {

    /**
     * 处理监听器抛出的异常
     *
     * @param t 异常
     */
    void handleError(Throwable t);
}
