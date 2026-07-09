package com.minispring.event;

import java.util.EventObject;

/**
 * 应用事件基类
 * 所有自定义事件与生命周期事件都继承自本类。
 * source 通常为发布事件的容器本身。
 */
public abstract class ApplicationEvent extends EventObject {

    public ApplicationEvent(Object source) {
        super(source);
    }
}
