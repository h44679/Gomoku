package com.wuzi.server;

import com.wuzi.common.AnsiColor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String time() {
        return AnsiColor.color(
                "[" + LocalDateTime.now().format(FORMATTER) + "] ",
                AnsiColor.WHITE
        );
    }

    public static void info(String msg) {
        System.out.println(time() + AnsiColor.color("[INFO] " + msg, AnsiColor.BLUE));
    }

    public static void success(String msg) {
        System.out.println(time() + AnsiColor.color("[SUCCESS] " + msg, AnsiColor.GREEN));
    }

    public static void warn(String msg) {
        System.out.println(time() + AnsiColor.color("[WARN] " + msg, AnsiColor.YELLOW));
    }

    public static void error(String msg) {
        System.out.println(time() + AnsiColor.color("[ERROR] " + msg, AnsiColor.RED));
    }

    public static void error(String msg, Exception e) {
        error(msg);
        e.printStackTrace();
    }

    // --- 玩家操作相关 ---
    public static void playerEnterRoom(String playerName, int roomId) {
        info("玩家 " + playerName + " 进入房间 " + roomId);
    }

    public static void playerStartGame(String playerName, int roomId) {
        success("玩家 " + playerName + " 在房间 " + roomId + " 开始游戏");
    }

    public static void playerMove(String playerName, int roomId, int r, int c) {
        info("玩家 " + playerName + " 在房间 " + roomId + " 落子 (" + r + "," + c + ")");
    }

    public static void playerExitRoom(String playerName, int roomId) {
        warn("玩家 " + playerName + " 离开房间 " + roomId);
    }

    public static void roomCreated(int roomId) {
        info("房间 " + roomId + " 已创建");
    }
}
