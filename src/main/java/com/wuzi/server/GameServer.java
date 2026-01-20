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
            // 题25：线程池的作用和设计
            // 为什么使用线程池？
            // 1.每个连接需要单独线程处理（ClientHandler implements Runnable）
            // 2.如果为每个客户端创建新线程会导致：资源耗尽、频繁创建销毁线程（性能低下）
            // 3.线程池通过复用线程提高性能：新连接来时直接分配空闲线程，无需创建新线程
            // 4.选择newFixedThreadPool(20)的原因：
            //   - 容量固定为20，足够支撑多桌五子棋对战（每桌需2个玩家的处理线程）
            //   - 防止无限创建线程导致内存溢出或系统瘫痪
            //   - 对轻量级的五子棋应用来说20线程是合适的平衡
            threadPool = Executors.newFixedThreadPool(20);
            ServerLogger.success("五子棋服务端启动成功，监听端口：" + port);

            while (true) {
                // 题18：服务器接受客户端连接
                // Socket clientSocket = serverSocket.accept()的作用
                // 1.accept()是阻塞方法，等待客户端发起连接请求
                // 2.当有客户端连接时，accept()返回一个新的Socket对象代表该连接
                // 3.该Socket对象用于服务端和该客户端的后续通信（读写消息）
                // 4.将Socket传给ClientHandler进行处理（每个客户端对应一个ClientHandler）
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