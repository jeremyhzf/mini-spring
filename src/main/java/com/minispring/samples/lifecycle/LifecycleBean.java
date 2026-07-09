package com.minispring.samples.lifecycle;

import com.minispring.factory.lifecycle.DisposableBean;
import com.minispring.factory.lifecycle.InitializingBean;

/**
 * 演示初始化/销毁生命周期回调的 Bean
 */
public class LifecycleBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println("   [初始化] afterPropertiesSet() 被调用，属性注入完成");
    }

    @Override
    public void destroy() {
        System.out.println("   [销毁] destroy() 被调用，释放资源");
    }
}
