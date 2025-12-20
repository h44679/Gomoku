package com.wuzi.server;

import com.wuzi.common.AnsiColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String msg) {
        System.out.println(AnsiColor.color("[" + LocalDateTime.now().format(FORMATTER) + "] [INFO] " + msg, AnsiColor.BLUE));
    }

    public static void success(String msg) {
        System.out.println(AnsiColor.color("[" + LocalDateTime.now().format(FORMATTER) + "] [SUCCESS] " + msg, AnsiColor.GREEN));
    }

    public static void warn(String msg) {
        System.out.println(AnsiColor.color("[" + LocalDateTime.now().format(FORMATTER) + "] [WARN] " + msg, AnsiColor.YELLOW));
    }

    public static void error(String msg) {
        System.out.println(AnsiColor.color("[" + LocalDateTime.now().format(FORMATTER) + "] [ERROR] " + msg, AnsiColor.RED));
    }

    public static void error(String msg, Exception e) {
        error(msg);
        e.printStackTrace();
    }
}