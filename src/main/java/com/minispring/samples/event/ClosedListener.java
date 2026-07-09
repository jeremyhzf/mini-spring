package com.minispring.samples.event;

import com.minispring.event.ApplicationListener;
import com.minispring.event.ContextClosedEvent;
import com.minispring.stereotype.Component;

/**
 * 容器关闭监听器
 */
@Component
public class ClosedListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("[监听器] 容器关闭（ContextClosedEvent）");
    }
}
