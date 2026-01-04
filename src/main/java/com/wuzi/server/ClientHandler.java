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
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;

            while ((msg = in.readLine()) != null) {
                String[] parts = msg.split(" ");
                if (parts.length == 0) continue;

                ServerLogger.info("收到玩家[" + (player != null ? player.getName() : "未知") + "]命令：" + msg);
                switch (parts[0]) {
                    case "nickname":
                        handleNickname(parts);
                        break;
                    case "help":
                        // 服务端也可返回帮助信息（可选，客户端已本地显示）
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
                    case "leave":      // 新增离开房间指令
                        handleLeaveRoom();
                        break;
                    case "exit":
                        handleExit();
                        return;
                    default:
                        out.println(AnsiColor.color("无效指令！输入 help 查看所有支持的指令", AnsiColor.RED));
                        break;
                }
            }
        } catch (IOException e) {
            handleDisconnect();
        } finally {
            closeResources();
        }
    }

    private void handleAiStart() {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！", AnsiColor.RED));
            return;
        }

        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom != null && currentRoom.getPlayerCount() >= 2) {
            // 房间已满，有玩家在玩，拒绝开启本地 AI
            out.println(AnsiColor.color("房间已有人，无法开启 AI 对战，请退出房间或等待空闲房间", AnsiColor.RED));
            return;
        }

        // 可以开始本地 AI 对战逻辑
        out.println(AnsiColor.color("AI 对战模式已开启，你执黑(●)，AI执白(○)", AnsiColor.CYAN));
        // ⭐ 如果需要，你可以设置房间锁定，或者直接在客户端开启本地 AI
    }

    /**
     * 服务端返回帮助信息（客户端输入help时触发）
     */
    private void sendHelpInfo() {
        out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        out.println(AnsiColor.color("help          - 查看所有指令说明", AnsiColor.CYAN));
        out.println(AnsiColor.color("ls rooms      - 查看所有房间状态（人数/游戏状态）", AnsiColor.CYAN));
        out.println(AnsiColor.color("enter room X  - 加入X号房间", AnsiColor.CYAN));
        out.println(AnsiColor.color("start         - 开始游戏", AnsiColor.CYAN));
        out.println(AnsiColor.color("ai start      - 开始人机游戏", AnsiColor.CYAN));
        out.println(AnsiColor.color("put X Y       - 落子", AnsiColor.CYAN));
        out.println(AnsiColor.color("leave         - 离开当前房间，返回大厅", AnsiColor.CYAN));
        out.println(AnsiColor.color("exit          - 与服务器断开连接", AnsiColor.CYAN));
        out.println(AnsiColor.color("==============================\n", AnsiColor.CYAN));
    }

    private void handleNickname(String[] parts) {
        String nickname = parts.length > 1 ? parts[1].trim() : "匿名玩家";
        player = new Player(nickname, socket, out);
        ServerLogger.info("玩家[" + nickname + "]连接成功");
        out.println(AnsiColor.color(
                "欢迎 " + nickname + "！五子棋对战大厅已开启，输入 ls rooms 查看房间状态，输入 help 查看指令",
                AnsiColor.BLUE
        ));
    }

    private void handleListRooms(String[] parts) {
        if (parts.length > 1 && parts[1].equals("rooms")) {
            out.println(roomManager.getRoomsStatus());
        } else {
            out.println(AnsiColor.color("指令错误！正确格式：ls rooms | 输入 help 查看帮助", AnsiColor.RED));
        }
    }

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

    private void handleMakeMove(String[] parts) {
        if (player == null) {
            out.println(AnsiColor.color("请先设置昵称！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom == null) {
            out.println(AnsiColor.color("请先加入房间！输入 help 查看帮助", AnsiColor.RED));
            return;
        }
        if (parts.length != 3) {
            out.println(AnsiColor.color("落子格式错误！正确格式：put 7 A | 输入 help 查看帮助", AnsiColor.RED));
            return;
        }

        String xStr = parts[1];
        String yStr = parts[2];
        String playerColor = player.getColor();

        String result = currentRoom.makeMove(xStr, yStr, playerColor, player);
        out.println(result);

        Player opponent = (currentRoom.getPlayer1() == player) ? currentRoom.getPlayer2() : currentRoom.getPlayer1();
        if (opponent != null) {
            opponent.sendMessage(result);
            opponent.sendMessage(currentRoom.getBoard().toString());
        }
        // ⭐ 新增：提示对手该下棋了
        opponent.sendMessage(
                AnsiColor.color("轮到你下棋了", AnsiColor.BLUE)
        );
    }

    private void handleExit() {
        if (player != null) {
            roomManager.removePlayerFromRoom(player);
            ServerLogger.info("玩家[" + player.getName() + "]主动退出");
            out.println(AnsiColor.color("已退出游戏！", AnsiColor.GREEN));
        }
    }

    private void handleDisconnect() {
        if (player != null) {
            roomManager.removePlayerFromRoom(player);
            ServerLogger.warn("玩家[" + player.getName() + "]网络断线");
        }
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            ServerLogger.error("关闭客户端资源失败", e);
        }
    }

    // 在 ClientHandler 类里添加
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
        // ⭐ 1. 先找到对手（还没移除之前）
        Player opponent = null;
        if (currentRoom.getPlayer1() == player) {
            opponent = currentRoom.getPlayer2();
        } else if (currentRoom.getPlayer2() == player) {
            opponent = currentRoom.getPlayer1();
        }

        // ⭐ 2. 通知对手
        if (opponent != null) {
            opponent.sendMessage(
                    AnsiColor.color("您的对手已离开房间", AnsiColor.BLUE)
            );
        }
        // 从房间移除玩家
        roomManager.removePlayerFromRoom(player);

        // 给玩家提示
        out.println(AnsiColor.color("已离开房间，返回大厅", AnsiColor.GREEN));

        // 日志记录
        ServerLogger.info("玩家[" + player.getName() + "]离开房间[" + currentRoom.getRoomId() + "]返回大厅");
    }

}