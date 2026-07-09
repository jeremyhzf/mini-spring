package com.minispring.samples.lifecycle;

import com.minispring.factory.scope.ScopeAnnotation;

/**
 * 标注为 prototype 作用域：每次获取都是新实例
 */
@ScopeAnnotation("prototype")
public class PrototypeBean {
}
