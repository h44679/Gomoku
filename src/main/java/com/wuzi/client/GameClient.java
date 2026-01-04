package com.wuzi.client;

import com.wuzi.ai.GomokuAI;
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

    // 本地人机对战相关
    private final int SIZE = 15;
    private int[][] localBoard = new int[SIZE][SIZE];
    private boolean isAiMode = false;
    private final int HUMAN = 1; // 玩家为黑
    private final int AI = 2;    // AI 为白
    private GomokuAI gomokuAI = new GomokuAI();

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
                    showHelpInfo();
                } else if (input.equalsIgnoreCase("ai start")) {
                    isAiMode = true;
                    resetLocalBoard();
                    System.out.println(AnsiColor.color("本地人机对战模式已开启。你执黑(●)，AI执白(○)。", AnsiColor.CYAN));
                    printLocalBoard();
                } else if (input.startsWith("put ")) {
                    String[] parts = input.split(" ");
                    if (parts.length != 3) {
                        System.out.println(AnsiColor.color("落子格式错误！正确格式：put 7 A 或 put A 7", AnsiColor.RED));
                        continue;
                    }
                    if (isAiMode) {
                        int[] rc = parseInput(parts[1], parts[2]);
                        if (rc == null || !isValidMove(rc[0], rc[1])) {
                            System.out.println(AnsiColor.color("无效坐标或该位置已被占用！", AnsiColor.RED));
                            continue;
                        }
                        localBoard[rc[0]][rc[1]] = HUMAN;
                        printLocalBoard();
                        if (checkWin(rc[0], rc[1], HUMAN)) {
                            System.out.println(AnsiColor.color("恭喜你，五连！你赢了！", AnsiColor.YELLOW));
                            isAiMode = false;
                            continue;
                        }
                        // AI 落子
                        int[] aiMove = gomokuAI.getNextStep(localBoard, AI);
                        if (aiMove[0] != -1 && aiMove[1] != -1) {
                            localBoard[aiMove[0]][aiMove[1]] = AI;
                            System.out.println(AnsiColor.color("AI 落子: " + idxToLabel(aiMove[0]) + " " + idxToLabel(aiMove[1]), AnsiColor.CYAN));
                            printLocalBoard();
                            if (checkWin(aiMove[0], aiMove[1], AI)) {
                                System.out.println(AnsiColor.color("AI 五连，游戏结束！", AnsiColor.YELLOW));
                                isAiMode = false;
                            }
                        }
                    } else {
                        out.println(input);
                    }
                } else if (input.equalsIgnoreCase("ls rooms") || input.startsWith("enter room ") || input.equalsIgnoreCase("start")) {
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

    // 重置本地棋盘
    private void resetLocalBoard() {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                localBoard[i][j] = 0;
    }

    // 棋盘打印，A-O/0-14坐标，+空，●黑，○白
    private void printLocalBoard() {
        System.out.print("   ");
        for (int c = 0; c < SIZE; c++) {
            System.out.print(" " + idxToLabel(c));
        }
        System.out.println();
        for (int r = 0; r < SIZE; r++) {
            System.out.printf("%2s ", idxToLabel(r));
            for (int c = 0; c < SIZE; c++) {
                char ch = '+';
                if (localBoard[r][c] == HUMAN) ch = '●';
                else if (localBoard[r][c] == AI) ch = '○';
                System.out.print(" " + ch);
            }
            System.out.println();
        }
    }

    // 坐标解析，支持 put 7 A 或 put A 7
    private int[] parseInput(String s1, String s2) {
        Integer r = labelToIdx(s1);
        Integer c = labelToIdx(s2);
        if (r != null && c != null) return new int[]{r, c};
        r = labelToIdx(s2);
        c = labelToIdx(s1);
        if (r != null && c != null) return new int[]{r, c};
        return null;
    }

    // 坐标合法性
    private boolean isValidMove(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE && localBoard[r][c] == 0;
    }

    // 胜负判定
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

    // 行列号转A-O或数字
    private String idxToLabel(int idx) {
        if (idx >= 0 && idx < 10) return String.valueOf(idx);
        if (idx >= 10 && idx < 15) return String.valueOf((char)('A' + idx - 10));
        return "?";
    }

    // A-O/0-14转数字
    private Integer labelToIdx(String s) {
        s = s.toUpperCase();
        if (s.length() == 1) {
            char ch = s.charAt(0);
            if (ch >= '0' && ch <= '9') return ch - '0';
            if (ch >= 'A' && ch <= 'O') return ch - 'A' + 10;
        }
        try {
            int v = Integer.parseInt(s);
            if (v >= 0 && v < SIZE) return v;
        } catch (Exception ignored) {}
        return null;
    }

    private void showHelpInfo() {
        System.out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("help          - 查看所有指令说明", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("ls rooms      - 查看所有房间状态", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("enter room X  - 加入X号房间", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("start         - 开始游戏", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("put X Y       - 落子（本地/联网均支持A-O或0-14）", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("ai start      - 开启本地人机对战模式", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("exit          - 退出游戏", AnsiColor.GREEN));
        System.out.println(AnsiColor.color("==============================\n", AnsiColor.CYAN));
    }

    private void listenServerMessage() {
        String msg;
        try {
            while ((msg = in.readLine()) != null) {
                if (!isAiMode) System.out.println(msg);
            }
        } catch (IOException e) {
            System.out.println(AnsiColor.color("与服务端断开连接", AnsiColor.RED));
        }
    }

    public static void main(String[] args) {
        new GameClient("127.0.0.1", 8888);
    }
}
