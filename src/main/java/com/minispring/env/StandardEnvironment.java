package com.minispring.env;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标准环境实现
 */
public class StandardEnvironment implements Environment {

    // 配置存储
    private final Map<String, String> properties = new HashMap<>();
    private final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]+))?}");

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * 解析占位符
     * @param text 待解析的文本   如 ${db.url:jdbc:mysql://localhost}
     * @return 解析后的文本   如 db.url为null, 则为默认值 jdbc:mysql://localhost
     */
    @Override
    public String resolvePlaceholders(String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

        StringBuilder result = new StringBuilder();
        // 在文本中逐个寻找符合正则的占位符，找到就进入循环。
        while (matcher.find()) {
            // 提取占位符中的键（key）。例如在 ${db.url} 中提取 db.url。
            String key = matcher.group(1);
            // 提取占位符中的默认值（default value）。如果没有写默认值，这里会是 null。
            String defaultValue = matcher.group(2);
            // 获取键所对应的值。
            String value = getProperty(key, defaultValue != null ? defaultValue : "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
