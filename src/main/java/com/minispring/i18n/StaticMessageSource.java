package com.minispring.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 内存消息源：用 Map 存消息，便于测试与快速使用
 */
public class StaticMessageSource extends AbstractMessageSource {

    private final Map<Locale, Map<String, String>> messages = new HashMap<>();

    /**
     * 添加一条消息
     *
     * @param code    消息码
     * @param locale  语言
     * @param message 消息 pattern（可含 {0} 占位符）
     */
    public void addMessage(String code, Locale locale, String message) {
        messages.computeIfAbsent(locale, k -> new HashMap<>()).put(code, message);
    }

    @Override
    protected String resolveCode(String code, Locale locale) {
        Map<String, String> byCode = messages.get(locale);
        return (byCode == null) ? null : byCode.get(code);
    }
}
