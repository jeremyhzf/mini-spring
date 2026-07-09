package com.minispring.samples.messagesource;

import com.minispring.factory.DefaultBeanContainer;
import com.minispring.i18n.ResourceBundleMessageSource;

import java.util.Locale;

/**
 * 阶段7-3 - 国际化示例
 * 演示：ResourceBundleMessageSource 读 .properties、多 locale、{0} 参数、@Autowired 注入
 */
public class MessageSourceDemo {

    public static void main(String[] args) {
        System.out.println("=== 阶段7-3：国际化 MessageSource 示例 ===\n");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");

        DefaultBeanContainer container = new DefaultBeanContainer();
        container.setMessageSource(messageSource);
        container.scanComponents("com.minispring.samples.messagesource");

        GreetingService service = (GreetingService) container.getBean("greetingService");

        System.out.println("English : " + service.greet("Alice", Locale.ENGLISH));
        System.out.println("中文     : " + service.greet("Alice", Locale.CHINESE));

        System.out.println("\n=== 阶段7-3 示例结束 ===");
    }
}
