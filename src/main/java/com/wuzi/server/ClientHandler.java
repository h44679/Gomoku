package com.wuzi.server;

import com.wuzi.common.AnsiColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;
    private RoomManager roomManager;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            // 初始化输入输出流
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;

            // 循环读取客户端指令
            while ((msg = in.readLine()) != null) {
                String[] parts = msg.split(" ");
                if (parts.length == 0) continue;

                // 日志记录：收到玩家指令
                ServerLogger.info("收到玩家[" + (player != null ? player.getName() : "未知") + "]命令：" + msg);

                // 指令分发
                switch (parts[0]) {
                    case "nickname":
                        handleNickname(parts);
                        break;
                    case "help":
                        sendHelpInfo();
                        break;
                    case "ls":
                        handleListRooms(parts);
                        break;
                    case "enter":
                        handleEnterRoom(parts);
                        break;
                    case "start":
                        handleStartGame();
                        break;
                    case "put":
                        handleMakeMove(parts);
                        break;
                    case "leave":
                        handleLeaveRoom();
                        break;
                    case "exit":
                        handleExit();
                        return; // 退出循环，关闭连接
                    case "again":   // 新增
                        handleAgain();
                        break;
                    default:
                        out.println(AnsiColor.color("无效指令！输入 help 查看所有支持的指令", AnsiColor.RED));
                        break;
                }
            }
        } catch (IOException e) {
            // 网络异常：处理玩家断线
            handleDisconnect();
        } finally {
            // 释放资源
            closeResources();
        }
    }
    private void handleAgain() {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }

        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom == null) {
            out.println(AnsiColor.color("你不在任何房间，无法再来一局！", AnsiColor.RED));
            return;
        }

        if (!currentRoom.isGameOver()) {
            out.println(AnsiColor.color("当前游戏还未结束，无法再来一局！", AnsiColor.RED));
            return;
        }

        // 核心调用：请求再来一局
        currentRoom.requestAgain(player);
    }

    /**
     * 处理AI对战开始指令（预留逻辑）
     */
    private void handleAiStart() {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！", AnsiColor.RED));
            return;
        }

        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom != null && currentRoom.getPlayerCount() >= 2) {
            out.println(AnsiColor.color("房间已有人，无法开启 AI 对战，请退出房间或等待空闲房间", AnsiColor.RED));
            return;
        }

        out.println(AnsiColor.color("AI 对战模式已开启，你执黑(●)，AI执白(○)", AnsiColor.CYAN));
    }

    /**
     * 发送帮助信息给客户端
     */
    private void sendHelpInfo() {
        out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        out.println(AnsiColor.color("【 房间管理 】", AnsiColor.YELLOW));
        out.println(AnsiColor.color("ls rooms      - 查看所有房间状态（人数/游戏状态）", AnsiColor.CYAN));
        out.println(AnsiColor.color("enter room X  - 加入X号房间", AnsiColor.CYAN));
        out.println(AnsiColor.color("leave         - 离开当前房间，返回大厅", AnsiColor.CYAN));
        out.println(AnsiColor.color("【 游戏操作 】", AnsiColor.YELLOW));
        out.println(AnsiColor.color("start         - 开始游戏", AnsiColor.CYAN));
        out.println(AnsiColor.color("ai start      - 开始人机游戏", AnsiColor.CYAN));
        out.println(AnsiColor.color("again         - 再来一局", AnsiColor.CYAN));
        out.println(AnsiColor.color("put X Y       - 落子", AnsiColor.CYAN));
        out.println(AnsiColor.color("【 系统 】", AnsiColor.YELLOW));
        out.println(AnsiColor.color("help          - 查看所有指令说明", AnsiColor.CYAN));
        out.println(AnsiColor.color("exit          - 与服务器断开连接", AnsiColor.CYAN));
        out.println(AnsiColor.color("==============================\n", AnsiColor.CYAN));
    }

    /**
     * 处理设置昵称指令
     */
    private void handleNickname(String[] parts) {
        String nickname = parts.length > 1 ? parts[1].trim() : "匿名玩家";
        // 创建玩家实例（适配你的Player构造方法）
        player = new Player(nickname, socket, out);
        ServerLogger.info("玩家[" + nickname + "]连接成功");
        out.println(AnsiColor.color(
                "欢迎 " + nickname + "！五子棋对战大厅已开启，输入 ls rooms 查看房间状态，输入 help 查看指令",
                AnsiColor.BLUE
        ));
    }

    /**
     * 处理查看房间列表指令
     */
    private void handleListRooms(String[] parts) {
        if (parts.length > 1 && parts[1].equals("rooms")) {
            out.println(roomManager.getRoomsStatus());
        } else {
            out.println(AnsiColor.color("指令错误！正确格式：ls rooms | 输入 help 查看帮助", AnsiColor.RED));
        }
    }

    /**
     * 处理加入房间指令
     */
    private void handleEnterRoom(String[] parts) {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        if (parts.length >= 3 && parts[1].equals("room")) {
            try {
                int roomId = Integer.parseInt(parts[2]);
                GameRoom room = roomManager.getRoom(roomId);
                if (room == null) {
                    out.println(AnsiColor.color("房间 " + roomId + " 不存在！输入 help 查看帮助", AnsiColor.RED));
                    return;
                }
                boolean success = room.addPlayer(player);
                if (success) {
                    if (room.getPlayer1() == null || room.getPlayer2() == null) {
                        out.println(AnsiColor.color("房间未满，等待其他玩家加入，或输入“ai start”开始人机对战", AnsiColor.YELLOW));
                    }
                } else {
                    out.println(AnsiColor.color("加入房间失败！房间已满", AnsiColor.RED));
                }
            } catch (NumberFormatException e) {
                out.println(AnsiColor.color("房间号必须是数字！输入 help 查看帮助", AnsiColor.RED));
            }
        } else {
            out.println(AnsiColor.color("指令错误！正确格式：enter room 1 | 输入 help 查看帮助", AnsiColor.RED));
        }
    }

    /**
     * 处理开始游戏指令（玩家准备）
     */
    private void handleStartGame() {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom == null) {
            out.println(AnsiColor.color("请先加入房间！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        currentRoom.playerReady(player);
    }

    /**
     * 核心：处理落子指令（修复所有问题，实现需求）
     */
    private void handleMakeMove(String[] parts) {
        // 1. 基础校验：玩家未初始化/未加入房间
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom == null) {
            out.println(AnsiColor.color("请先加入房间！输入 help 查看帮助", AnsiColor.RED));
            return;
        }

        // 2. 游戏状态校验：游戏已结束则拒绝落子
        if (currentRoom.isGameOver()) {
            out.println(AnsiColor.color("游戏已经结束，无法落子！", AnsiColor.RED));
            return;
        }

        // 3. 指令格式校验
        if (parts.length != 3) {
            out.println(AnsiColor.color("落子格式错误！正确格式：put A 7 | 输入 help 查看帮助", AnsiColor.RED));
            return;
        }

        // 4. 解析坐标 & 玩家颜色
        String xStr = parts[1];
        String yStr = parts[2];
        String playerColor = player.getColor();
        if (playerColor == null || playerColor.isEmpty()) {
            out.println(AnsiColor.color("棋子颜色未分配！请重新加入房间", AnsiColor.RED));
            return;
        }

        // 5. 落子
        String result = currentRoom.makeMove(xStr, yStr, playerColor, player);
        if (result.startsWith(AnsiColor.RED)) { // 错误提示直接返回
            out.println(result);
            return;
        }

        // ========== 清屏刷新棋盘 ==========
        StringBuilder boardOutput = new StringBuilder();
        boardOutput.append("\u001B[2J"); // 清屏
        boardOutput.append("\u001B[H");  // 光标移到左上
        boardOutput.append(currentRoom.getBoard().toString()); // 打印棋盘

        // 6. 给自己发送
        out.println(boardOutput.toString());

        // 7. 结果提示
        String coloredResult;
        if (currentRoom.isGameOver()) {
            coloredResult = AnsiColor.color(result, AnsiColor.GREEN); // 获胜提示
        } else {
            coloredResult = AnsiColor.color(result, AnsiColor.CYAN);  // 普通落子
        }
        out.println(coloredResult);

        if (currentRoom.isGameOver()) {
            out.println(AnsiColor.color("游戏结束！输入 leave 离开房间，或者输入 again 再来一局...", AnsiColor.BLUE));
        }

        // 8. 给对手同步消息
        Player opponent = (currentRoom.getPlayer1() == player) ? currentRoom.getPlayer2() : currentRoom.getPlayer1();
        if (opponent != null) {
            StringBuilder oppOutput = new StringBuilder();
            oppOutput.append("\u001B[2J"); // 清屏
            oppOutput.append("\u001B[H");
            oppOutput.append(currentRoom.getBoard().toString());

            opponent.sendMessage(oppOutput.toString());
            opponent.sendMessage(coloredResult);

            if (!currentRoom.isGameOver()) {
                opponent.sendMessage(AnsiColor.color("轮到你下棋了", AnsiColor.BLUE));
                player.sendMessage(AnsiColor.color("当前不该你下棋，等待对手下棋...", AnsiColor.BLUE));
            } else {
                opponent.sendMessage(AnsiColor.color("游戏结束！输入 leave 离开房间，或者输入 again 再来一局...", AnsiColor.BLUE));
            }
        }
    }


    /**
     * 处理退出游戏指令
     */
    private void handleExit() {
        if (player != null) {
            roomManager.removePlayerFromRoom(player);
            ServerLogger.info("玩家[" + player.getName() + "]断开连接");
            out.println(AnsiColor.color("已退出游戏！", AnsiColor.GREEN));
        }
    }

    /**
     * 处理玩家断线
     */
    private void handleDisconnect() {
        if (player != null) {
            roomManager.removePlayerFromRoom(player);
            ServerLogger.warn("玩家[" + player.getName() + "]网络断线");
        }
    }

    /**
     * 释放客户端资源
     */
    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            ServerLogger.error("关闭客户端资源失败", e);
        }
    }

    /**
     * 处理离开房间指令
     */
    private void handleLeaveRoom() {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }

        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom == null) {
            out.println(AnsiColor.color("你不在任何房间，已在大厅", AnsiColor.YELLOW));
            return;
        }

        // 通知对手
        Player opponent = (currentRoom.getPlayer1() == player) ? currentRoom.getPlayer2() : currentRoom.getPlayer1();
        if (opponent != null) {
            opponent.sendMessage(AnsiColor.color("您的对手已离开房间", AnsiColor.BLUE));
        }

        // 移除玩家并提示
        roomManager.removePlayerFromRoom(player);
        out.println(AnsiColor.color("已离开房间，返回大厅", AnsiColor.GREEN));
        ServerLogger.info("玩家[" + player.getName() + "]离开房间[" + currentRoom.getRoomId() + "]返回大厅");
    }

    // 替换 ClientHandler 中错误的 destroyRoom 方法
    public synchronized boolean destroyRoom(int roomId) {
        // 修复：通过 roomManager 调用销毁逻辑（roomMap 属于 RoomManager）
        return roomManager.destroyRoom(roomId);
    }
}
