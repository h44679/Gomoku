package com.wuzi.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class GameServer {
    private ServerSocket serverSocket;
    private RoomManager roomManager;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            roomManager = new RoomManager();
            ServerLogger.success("五子棋服务端启动成功，监听端口：" + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, roomManager)).start();
            }
        } catch (IOException e) {
            ServerLogger.error("服务端启动失败：" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new GameServer(8888);
    }
}