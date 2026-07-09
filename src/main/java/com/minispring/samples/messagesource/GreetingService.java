package com.minispring.samples.messagesource;

import com.minispring.annotation.Autowired;
import com.minispring.i18n.MessageSource;
import com.minispring.stereotype.Service;

import java.util.Locale;

/**
 * 问候服务：依赖注入 MessageSource，按 locale 取问候语
 */
@Service
public class GreetingService {

    @Autowired
    private MessageSource messageSource;

    public String greet(String name, Locale locale) {
        return messageSource.getMessage("greeting", new Object[]{name}, locale);
    }
}
