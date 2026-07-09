package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StaticMessageSource 测试：内存消息、locale 命中、参数替换、未命中语义
 */
public class StaticMessageSourceTest {

    @Test
    void shouldResolveAddedMessageWithArgs() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        assertEquals("你好,Alice", src.getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE));
    }

    @Test
    void shouldResolveByLocale() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.ENGLISH, "Hello,{0}");
        src.addMessage("greeting", Locale.CHINESE, "你好,{0}");

        assertEquals("Hello,Bob", src.getMessage("greeting", new Object[]{"Bob"}, Locale.ENGLISH));
        assertEquals("你好,Bob", src.getMessage("greeting", new Object[]{"Bob"}, Locale.CHINESE));
    }

    @Test
    void shouldMissForUnknownLocaleOrCode() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("greeting", Locale.ENGLISH, "Hello");

        // 中文 locale 下没有消息
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("greeting", null, Locale.CHINESE));
        // 未知 code
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenMissing() {
        StaticMessageSource src = new StaticMessageSource();
        assertEquals("D", src.getMessage("missing", null, "D", Locale.ENGLISH));
    }

    @Test
    void shouldHandleNullArgsWhenNoPlaceholder() {
        StaticMessageSource src = new StaticMessageSource();
        src.addMessage("plain", Locale.ENGLISH, "Hi");
        assertEquals("Hi", src.getMessage("plain", null, Locale.ENGLISH));
    }
}
