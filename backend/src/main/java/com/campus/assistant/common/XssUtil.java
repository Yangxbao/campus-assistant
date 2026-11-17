package com.campus.assistant.common;

/**
 * XSS 过滤工具：对用户输入文本做 HTML 实体转义，防止存储型 XSS。
 * 保存用户文本（标题、描述、评价内容、备注等）前统一调用。
 */
public final class XssUtil {

    private XssUtil() {
    }

    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                case '&' -> sb.append("&amp;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
