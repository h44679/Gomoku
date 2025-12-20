package com.wuzi.client;

import com.wuzi.common.AnsiColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public GameClient(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(AnsiColor.color("成功连接到五子棋服务端", AnsiColor.GREEN));

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("请输入你的昵称：");
            String nickname = console.readLine().trim();
            out.println("nickname " + nickname);

            new Thread(this::listenServerMessage).start();

            String input;
            while ((input = console.readLine()) != null) {
                input = input.trim();
                if (input.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                } else if (input.equalsIgnoreCase("help")) {
                    // 客户端本地显示帮助信息（无需走服务端）
                    showHelpInfo();
                } else if (input.equalsIgnoreCase("ls rooms")) {
                    out.println(input);
                } else if (input.startsWith("enter room ")) {
                    out.println(input);
                } else if (input.equalsIgnoreCase("start")) {
                    out.println(input);
                } else if (input.startsWith("put ")) {
                    String[] parts = input.split(" ");
                    if (parts.length != 3) {
                        System.out.println(AnsiColor.color("落子格式错误！正确格式：put 7 A（行 列）", AnsiColor.RED));
                        continue;
                    }
                    out.println(input);
                } else {
                    System.out.println(AnsiColor.color("无效指令！输入 help 查看所有支持的指令", AnsiColor.RED));
                }
            }
        } catch (IOException e) {
            System.err.println(AnsiColor.color("连接服务端失败：" + e.getMessage(), AnsiColor.RED));
        } finally {
            try {
                if (socket != null) socket.close();
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示帮助信息
     */
    private void showHelpInfo() {
        System.out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("help          - 查看所有指令说明", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("ls rooms      - 查看所有房间状态（人数/游戏状态）", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("enter room X  - 加入X号房间（X为数字，如enter room 1）", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("start         - 开始游戏（需房间内有2名玩家）", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("put X Y       - 落子（X/Y为0-E，如put 7 A 或 put A 7）", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("exit          - 退出游戏", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("==============================\n", AnsiColor.CYAN));
    }

    private void listenServerMessage() {
        String msg;
        try {
            while ((msg = in.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            System.out.println(AnsiColor.color("与服务端断开连接", AnsiColor.RED));
        }
    }

    public static void main(String[] args) {
        new GameClient("127.0.0.1", 8888);
    }
}