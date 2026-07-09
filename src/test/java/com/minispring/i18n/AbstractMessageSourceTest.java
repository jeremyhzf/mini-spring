package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractMessageSource 测试：参数替换、命中/未命中语义、默认值重载
 * 用一个测试专用的具体子类提供 resolveCode
 */
public class AbstractMessageSourceTest {

    private AbstractMessageSource source() {
        return new AbstractMessageSource() {
            @Override
            protected String resolveCode(String code, Locale locale) {
                switch (code) {
                    case "greeting": return "Hello,{0}";
                    case "plain": return "Hi";
                    default: return null;
                }
            }
        };
    }

    @Test
    void shouldResolveAndSubstituteArgs() {
        AbstractMessageSource src = source();
        assertEquals("Hello,Alice", src.getMessage("greeting", new Object[]{"Alice"}, Locale.ENGLISH));
    }

    @Test
    void shouldHandleNullArgsWhenNoPlaceholder() {
        AbstractMessageSource src = source();
        assertEquals("Hi", src.getMessage("plain", null, Locale.ENGLISH),
            "args==null 应当作空数组，无占位符时原样返回");
    }

    @Test
    void shouldThrowWhenCodeMissing() {
        AbstractMessageSource src = source();
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenCodeMissing() {
        AbstractMessageSource src = source();
        assertEquals("fallback", src.getMessage("missing", null, "fallback", Locale.ENGLISH));
    }

    @Test
    void defaultMessageOverloadShouldStillResolveWhenCodePresent() {
        AbstractMessageSource src = source();
        assertEquals("Hi", src.getMessage("plain", null, "fallback", Locale.ENGLISH),
            "代码存在时默认值重载应返回解析结果而非默认值");
    }
}
