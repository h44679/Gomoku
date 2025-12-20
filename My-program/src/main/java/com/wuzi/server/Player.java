package com.wuzi.server;

import java.io.PrintWriter;
import java.net.Socket;

public class Player {
    private String name;
    private Socket socket;
    private PrintWriter out;
    private String color;
    private GameRoom currentRoom;
    private boolean isTestMode;

    public Player(String name, Socket socket, PrintWriter out) {
        this.name = name;
        this.socket = socket;
        this.out = out;
        this.isTestMode = (socket == null);
    }

    public void sendMessage(String msg) {
        if (isTestMode) {
            System.out.println(msg);
        } else if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public String getName() { return name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(GameRoom currentRoom) { this.currentRoom = currentRoom; }
    public boolean isTestMode() { return isTestMode; }
}