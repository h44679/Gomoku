package com.wuzi.server;

import java.io.PrintWriter;
import java.net.Socket;

public class Player {
    private String name;
    private Socket socket;
    private PrintWriter out;
    private String color;       // 棋子颜色：black/white
    private GameRoom currentRoom; // 当前所在房间
    private boolean isTestMode; // 测试模式标记

    // 构造方法（兼容测试模式和正常模式）
    public Player(String name, Socket socket, PrintWriter out) {
        this.name = name;
        this.socket = socket;
        this.out = out;
        this.isTestMode = (socket == null); // 测试模式：socket为null
    }

    /**
     * 发送消息给玩家（兼容测试模式）
     * @param msg 要发送的消息
     */
    public void sendMessage(String msg) {
        if (isTestMode) {
            System.out.println(msg); // 测试模式：打印到控制台
        } else if (out != null) {
            out.println(msg);
            out.flush(); // 强制刷新缓冲区，确保消息即时发送
        }
    }

    // Getter & Setter（完整且适配ClientHandler）
    public String getName() { return name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(GameRoom currentRoom) { this.currentRoom = currentRoom; }
    public boolean isTestMode() { return isTestMode; }

    // 补充：方便日志打印的toString方法（可选）
    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", room=" + (currentRoom != null ? currentRoom.getRoomId() : "无") +
                '}';
    }
}