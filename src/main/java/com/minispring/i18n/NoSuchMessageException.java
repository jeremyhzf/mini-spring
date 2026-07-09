package com.minispring.i18n;

import java.util.Locale;

/**
 * 找不到消息时抛出
 */
public class NoSuchMessageException extends RuntimeException {

    public NoSuchMessageException(String code, Locale locale) {
        super("No message found under code '" + code + "' for locale '" + locale + "'.");
    }
}
