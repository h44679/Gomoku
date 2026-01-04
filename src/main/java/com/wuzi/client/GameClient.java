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

    // 【修改点1】新增变量：记录最后落子的坐标，用于高亮显示
    private int lastRow = -1;
    private int lastCol = -1;

    public GameClient(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(AnsiColor.color("成功连接到五子棋服务端", AnsiColor.GREEN));

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(AnsiColor.color("请输入你的昵称：", AnsiColor.BLUE));
            String nickname = console.readLine().trim();
            out.println("nickname " + nickname);

            new Thread(this::listenServerMessage).start();

            String input;
            while ((input = console.readLine()) != null) {
                input = input.trim();

                if (input.equalsIgnoreCase("exit")) {
                    if (isAiMode) {
                        isAiMode = false;
                        resetLocalBoard();
                        System.out.println(AnsiColor.color("你已经退出 AI 对战，并返回大厅", AnsiColor.GREEN));
                    } else {
                        out.println("exit");
                        break;
                    }

                } else if (input.equalsIgnoreCase("help")) {
                    if (isAiMode) {
                        showHelpInfo();
                    } else {
                        out.println("help");
                    }
                } else if (input.equalsIgnoreCase("ai start")) {
                    if (!isAiMode) {
                        out.println("leave");
                        resetLocalBoard();
                        isAiMode = true;
                        System.out.println(AnsiColor.color("本地人机对战模式已开启。你执黑(●)，AI执白(○)。", AnsiColor.CYAN));
                        printLocalBoard();
                    }
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

                        // --- 玩家落子 ---
                        localBoard[rc[0]][rc[1]] = HUMAN;
                        // 【修改点2】记录最后落子位置
                        lastRow = rc[0];
                        lastCol = rc[1];

                        printLocalBoard();
                        if (checkWin(rc[0], rc[1], HUMAN)) {
                            System.out.println(AnsiColor.color("恭喜你，五连！你赢了！", AnsiColor.YELLOW));
                            isAiMode = false;
                            System.out.println(AnsiColor.color("你已经返回大厅", AnsiColor.GREEN));
                            continue;
                        }

                        // --- AI 落子 ---
                        int[] aiMove = gomokuAI.getNextStep(localBoard, AI);
                        if (aiMove[0] != -1 && aiMove[1] != -1) {
                            localBoard[aiMove[0]][aiMove[1]] = AI;

                            // 【修改点2】记录 AI 最后落子位置
                            lastRow = aiMove[0];
                            lastCol = aiMove[1];

                            // 【修改点3】AI 落子提示使用专门的格式化方法
                            // 修复：交换行列参数，使文字提示与棋盘视觉渲染的 A-O / 1-15 保持一致
                            String posStr = getColLabel(lastRow) + " " + getRowLabel(lastCol);
                            System.out.println(AnsiColor.color("AI 落子: " + posStr, AnsiColor.CYAN));

                            printLocalBoard();

                            if (checkWin(aiMove[0], aiMove[1], AI)) {
                                System.out.println(AnsiColor.color("AI 五连，游戏结束！", AnsiColor.YELLOW));
                                isAiMode = false;
                                System.out.println(AnsiColor.color("你已经返回大厅", AnsiColor.GREEN));
                            }
                        }

                    } else {
                        out.println(input); // 联网落子
                    }

                } else if (input.equalsIgnoreCase("leave")) {
                    if (isAiMode) {
                        isAiMode = false;
                        resetLocalBoard();
                        System.out.println(AnsiColor.color("你已经返回大厅", AnsiColor.GREEN));
                    } else {
                        out.println("leave");
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
        // 【修改点4】重置最后落子记录
        lastRow = -1;
        lastCol = -1;
    }

    // 棋盘打印
    // 【修改点5】使用 lastRow 和 lastCol 进行高亮渲染
    private void printLocalBoard() {
        System.out.println(gomokuAI.boardToString(localBoard, lastRow, lastCol));
    }

    // 【修改点6】辅助方法：行索引转数字字符串 (0->1, 14->15)
    private String getRowLabel(int r) {
        return String.valueOf(r + 1);
    }

    // 【修改点6】辅助方法：列索引转字母字符串 (0->A, 14->O)
    private String getColLabel(int c) {
        return String.valueOf((char)('A' + c));
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

    // A-O 转 0-14，或者 1-15 转 0-14
    private Integer labelToIdx(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.toUpperCase();

        char firstChar = s.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'O') {
            return firstChar - 'A';
        }

        try {
            int v = Integer.parseInt(s);
            v = v - 1;
            if (v >= 0 && v < SIZE) return v;
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private void showHelpInfo() {
        System.out.println(AnsiColor.color("\n===== 五子棋游戏指令帮助 =====", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("help          - 查看所有指令说明", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("ls rooms      - 查看所有房间状态（人数/游戏状态）", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("enter room X  - 加入X号房间", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("start         - 开始游戏", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("ai start      - 开始人机游戏", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("put X Y       - 落子 (例: put H 8)", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("leave         - 离开当前房间，返回大厅", AnsiColor.CYAN));
        System.out.println(AnsiColor.color("exit          - 与服务器断开连接", AnsiColor.CYAN));
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