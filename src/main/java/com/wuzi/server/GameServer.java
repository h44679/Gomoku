package com.wuzi.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private ServerSocket serverSocket;
    private RoomManager roomManager;
    private ExecutorService threadPool;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            roomManager = new RoomManager();
            //题目25：选newFixedThreadPool适配五子棋轻量对战场景，20 线程足够支撑多桌对战，又能控资源不浪费；
            threadPool = Executors.newFixedThreadPool(20);
            ServerLogger.success("五子棋服务端启动成功，监听端口：" + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket, roomManager));
            }
        } catch (IOException e) {
            ServerLogger.error("服务端启动失败：" + e.getMessage());
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    public static void main(String[] args) {
        new GameServer(8888);
    }

    public void shutdown() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            ServerLogger.error("关闭服务端失败", e);
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}