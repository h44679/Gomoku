package com.wuzi.common;

public class AnsiColor {
    // 重置样式
    public static final String RESET = "\u001B[0m";

    // 字体颜色
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // 背景颜色
    public static final String BG_RED = "\u001B[41m"; // 高亮最后一步

    /**
     * 给文本添加指定颜色，自动重置防止颜色泄漏
     * @param text 要着色的文本
     * @param colorCode 颜色码（如AnsiColor.RED）
     * @return 着色后的文本
     */
    public static String color(String text, String colorCode) {
        return colorCode + text + RESET;
    }
}
