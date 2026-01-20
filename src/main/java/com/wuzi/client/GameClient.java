package com.wuzi.client;

import com.wuzi.ai.GomokuAI;
import com.wuzi.common.AnsiColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * 满足老师要求的双线程架构：
 * 1. 主线程：负责处理用户输入（Scanner）并发送给服务器。
 * 2. 接收线程：独立运行，实时监听服务器发来的对手落子、棋盘刷新等消息。
 */
public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // --- 本地人机对战相关 ---
    private final int SIZE = 15;
    private int[][] localBoard = new int[SIZE][SIZE];
    private boolean isAiMode = false;
    private final int HUMAN = 1;
    private final int AI = 2;
    private GomokuAI gomokuAI = new GomokuAI();
    private int lastRow = -1;
    private int lastCol = -1;

    public GameClient(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);

            // 题20：客户端发送消息给服务器
            // PrintWriter是Java标准的网络输出流
            // 作用流程：
            // 1.out.println(message) 将消息写入输出缓冲区
            // 2.true参数（autoFlush=true）使println()自动刷新缓冲，确保消息即时发送
            // 3.服务端的ClientHandler在in.readLine()处接收该消息
            // 4.消息包括：昵称设置、房间指令（ls, enter）、游戏指令（put, start）等
            // 不使用flush()是因为PrintWriter设置了autoFlush，无需手动刷新
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(AnsiColor.color("成功连接到五子棋服务端", AnsiColor.GREEN));

            // 2. 初始化昵称（必须首先完成，符合 ClientHandler 逻辑）
            Scanner scanner = new Scanner(System.in);
            System.out.print(AnsiColor.color("请输入你的昵称：", AnsiColor.BLUE));
            String nickname = scanner.nextLine().trim();

            out.println("nickname " + nickname);

            // 题21：客户端接收线程的必要性
            // 为什么需要单独的线程来接收服务器消息？
            // 1.阻塞问题：in.readLine()是阻塞方法，没有消息时会一直等待
            //   如果在主线程中调用readLine()，主线程会被卡住无法处理用户输入
            // 2.并发需求：主线程需要处理用户的键盘输入（Scanner），接收线程负责实时接收服务消息
            //   两者同时进行才能实现"一边输入指令，一边接收对手落子和棋盘更新"的效果
            // 3.避免消息丢失：接收线程持续监听Socket，任何时刻服务端的消息都能及时接收
            //   否则若主线程正在等待输入，期间的服务消息会被缓存甚至丢失
            // 4.setDaemon(true)使其为守护线程，主线程退出时自动销毁，避免资源泄漏
            Thread receiveThread = new Thread(this::listenServerMessage);
            receiveThread.setDaemon(true); // 设置为守护线程，主线程退出它也退出
            receiveThread.start();

            // 4. 【主线程】处理循环输入
            handleUserInput(scanner);

        } catch (IOException e) {
            System.err.println(AnsiColor.color("连接服务端失败：" + e.getMessage(), AnsiColor.RED));
        } finally {
            closeResources();
        }
    }

    /**
     * 主线程逻辑：读取键盘指令
     */
    private void handleUserInput(Scanner scanner) {
        try {
            while (true) {
                // 为了视觉清晰，这里不加多余的提示符，让服务器发来的消息占主导
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    if (isAiMode) {
                        isAiMode = false;
                        resetLocalBoard();
                        System.out.println(AnsiColor.color("已退出 AI 模式，返回大厅", AnsiColor.GREEN));
                    } else {
                        out.println("exit");
                        break; // 退出循环，关闭程序
                    }
                } else if (input.equalsIgnoreCase("ai start")) {
                    startLocalAiMode();
                } else if (input.startsWith("put ")) {
                    handlePutCommand(input);
                } else {
                    // 通用命令直接转发给服务器（ls rooms, enter room X, start, leave, help）
                    if (!isAiMode) {
                        out.println(input);
                    } else if (input.equalsIgnoreCase("help")) {
                        showHelpInfo();
                    } else {
                        System.out.println(AnsiColor.color("AI 模式下不支持该指令", AnsiColor.RED));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("输入流异常");
        }
    }

    /**
     * 接收线程逻辑：死循环读取 Socket
     */
    private void listenServerMessage() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                // 如果当前不是本地 AI 模式，就打印服务器发来的所有内容（棋盘、公告等）
                if (!isAiMode) {
                    System.out.println(msg);
                }
            }
        } catch (IOException e) {
            System.out.println(AnsiColor.color("\n[系统] 与服务端断开连接", AnsiColor.RED));
        }
    }

    /**
     * 处理落子逻辑
     */
    private void handlePutCommand(String input) {
        String[] parts = input.split(" ");
        if (parts.length != 3) {
            System.out.println(AnsiColor.color("格式错误！例: put A 7", AnsiColor.RED));
            return;
        }

        if (isAiMode) {
            processAiMove(parts[1], parts[2]);
        } else {
            out.println(input); // 发送给服务器处理联网落子
        }
    }

    private void startLocalAiMode() {
        System.out.println(AnsiColor.color("开启本地人机对战...", AnsiColor.CYAN));
        out.println("leave"); // 通知服务器离开房间
        resetLocalBoard();
        isAiMode = true;
        printLocalBoard();
    }

    private void processAiMove(String p1, String p2) {
        int[] rc = parseInput(p1, p2);
        if (rc == null || !isValidMove(rc[0], rc[1])) {
            System.out.println(AnsiColor.color("无效坐标！", AnsiColor.RED));
            return;
        }

        // 玩家落子
        localBoard[rc[0]][rc[1]] = HUMAN;
        lastRow = rc[0]; lastCol = rc[1];
        printLocalBoard();

        if (checkWin(rc[0], rc[1], HUMAN)) {
            System.out.println(AnsiColor.color("你赢了！AI 已被击败。", AnsiColor.YELLOW));
            isAiMode = false;
            return;
        }

        // AI 思考并落子
        int[] aiMove = gomokuAI.getNextStep(localBoard, AI);
        if (aiMove[0] != -1) {
            localBoard[aiMove[0]][aiMove[1]] = AI;
            lastRow = aiMove[0]; lastCol = aiMove[1];
            System.out.println(AnsiColor.color("AI 落子: " + getColLabel(lastRow) + " " + getRowLabel(lastCol), AnsiColor.CYAN));
            printLocalBoard();
            if (checkWin(aiMove[0], aiMove[1], AI)) {
                System.out.println(AnsiColor.color("AI 赢了，再接再厉！", AnsiColor.YELLOW));
                isAiMode = false;
            }
        }
    }

    // --- 辅助工具方法 ---

    private void resetLocalBoard() {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                localBoard[i][j] = 0;
        lastRow = -1; lastCol = -1;
    }

    private void printLocalBoard() {
        System.out.println(gomokuAI.boardToString(localBoard, lastRow, lastCol));
    }

    private String getRowLabel(int r) { return String.valueOf(r + 1); }
    private String getColLabel(int c) { return String.valueOf((char)('A' + c)); }

    private int[] parseInput(String s1, String s2) {
        Integer r = labelToIdx(s1);
        Integer c = labelToIdx(s2);
        if (r != null && c != null) return new int[]{r, c};
        r = labelToIdx(s2);
        c = labelToIdx(s1);
        if (r != null && c != null) return new int[]{r, c};
        return null;
    }

    private Integer labelToIdx(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.toUpperCase();
        char first = s.charAt(0);
        if (first >= 'A' && first <= 'O') return first - 'A';
        try {
            int v = Integer.parseInt(s) - 1;
            if (v >= 0 && v < SIZE) return v;
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isValidMove(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE && localBoard[r][c] == 0;
    }

    private boolean checkWin(int r, int c, int color) {
        int[][] dirs = {{0,1},{1,0},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int cnt = 1;
            for (int k = 1; k < 5; k++) {
                int nr = r + d[0]*k, nc = c + d[1]*k;
                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE || localBoard[nr][nc] != color) break;
                cnt++;
            }
            for (int k = 1; k < 5; k++) {
                int nr = r - d[0]*k, nc = c - d[1]*k;
                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE || localBoard[nr][nc] != color) break;
                cnt++;
            }
            if (cnt >= 5) return true;
        }
        return false;
    }

    private void showHelpInfo() {
        System.out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("put X Y       - 落子 (例: put H 8)", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("exit          - 退出人机对战", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("==============================\n", AnsiColor.CYAN));
    }

    private void closeResources() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 记得联机时改为服务器实际 IP
        new GameClient("127.0.0.1", 8888);
    }
}