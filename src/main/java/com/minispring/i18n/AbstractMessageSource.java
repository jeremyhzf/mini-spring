package com.minispring.i18n;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * 消息源模板方法基类
 * 统一两个 getMessage 重载与 MessageFormat 参数替换；
 * 子类只需实现 resolveCode 提供原始 pattern。
 */
public abstract class AbstractMessageSource implements MessageSource {

    @Override
    public String getMessage(String code, Object[] args, Locale locale) {
        String pattern = resolveCode(code, locale);
        if (pattern == null) {
            throw new NoSuchMessageException(code, locale);
        }
        return format(pattern, args, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String pattern = resolveCode(code, locale);
        if (pattern == null) {
            return defaultMessage;
        }
        return format(pattern, args, locale);
    }

    /**
     * 子类实现：按 code + locale 返回原始 pattern；不存在返回 null
     */
    protected abstract String resolveCode(String code, Locale locale);

    private String format(String pattern, Object[] args, Locale locale) {
        Object[] fmtArgs = (args == null) ? new Object[0] : args;
        return new MessageFormat(pattern, locale)
                .format(fmtArgs, new StringBuffer(), null)
                .toString();
    }
}
