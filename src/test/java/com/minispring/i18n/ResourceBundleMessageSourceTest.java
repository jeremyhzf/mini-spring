package com.minispring.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceBundleMessageSource 测试：locale 命中、回退到根、参数替换、未命中、basename 缺失
 */
public class ResourceBundleMessageSourceTest {

    private ResourceBundleMessageSource source() {
        ResourceBundleMessageSource src = new ResourceBundleMessageSource();
        src.setBasename("i18n-test");
        return src;
    }

    @Test
    void shouldResolveEnglishBundle() {
        assertEquals("Hi,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.ENGLISH));
    }

    @Test
    void shouldResolveChineseBundle() {
        assertEquals("你好,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.CHINESE));
    }

    @Test
    void shouldFallbackToRootBundle() {
        // 法语 bundle 不存在，回退到根 bundle。
        // 注意：ResourceBundle.getBundle(base, locale) 会把 JVM 默认 locale 作为候选回退
        // （候选链：请求 locale → JVM 默认 locale → 根）。默认 en/zh 时会命中 _en/_zh
        // 而非根 bundle，导致本断言失败。故临时把默认 locale 设为无 bundle 的德语，
        // 使候选链真正回退到根 bundle，保证测试与运行环境默认 locale 无关。
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMAN);
            assertEquals("Hello,Alice", source().getMessage("greeting", new Object[]{"Alice"}, Locale.FRENCH));
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    void shouldResolveRootOnlyKeyWithFallback() {
        assertEquals("RootOnly", source().getMessage("only.root", null, Locale.ENGLISH));
    }

    @Test
    void shouldThrowWhenCodeMissing() {
        assertThrows(NoSuchMessageException.class,
            () -> source().getMessage("missing", null, Locale.ENGLISH));
    }

    @Test
    void shouldReturnDefaultWhenCodeMissing() {
        assertEquals("D", source().getMessage("missing", null, "D", Locale.ENGLISH));
    }

    @Test
    void shouldThrowWhenBasenameMissing() {
        ResourceBundleMessageSource src = new ResourceBundleMessageSource();
        src.setBasename("does-not-exist");
        assertThrows(NoSuchMessageException.class,
            () -> src.getMessage("any", null, Locale.ENGLISH),
            "basename 完全不存在时应抛 NoSuchMessageException 而非崩溃");
    }
}
