package com.minispring.samples.event;

import com.minispring.event.ApplicationListener;
import com.minispring.event.ContextRefreshedEvent;
import com.minispring.stereotype.Component;

/**
 * 容器就绪监听器
 */
@Component
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("[监听器] 容器就绪（ContextRefreshedEvent）");
    }
}
