package com.minispring.i18n;

import java.util.Locale;

/**
 * 国际化消息源接口
 * 按消息码 + locale 解析消息，支持 MessageFormat 参数替换（{0}、{1}…）
 */
public interface MessageSource {

    /**
     * 解析消息；找不到时抛 NoSuchMessageException
     *
     * @param code 消息码
     * @param args 占位符参数（可为 null）
     * @param locale 语言
     * @return 解析后的消息
     */
    String getMessage(String code, Object[] args, Locale locale);

    /**
     * 解析消息；找不到时返回 defaultMessage
     *
     * @param code           消息码
     * @param args           占位符参数（可为 null）
     * @param defaultMessage 找不到时的默认值
     * @param locale         语言
     * @return 解析后的消息，或 defaultMessage
     */
    String getMessage(String code, Object[] args, String defaultMessage, Locale locale);
}
