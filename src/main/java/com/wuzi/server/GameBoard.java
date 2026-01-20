package com.wuzi.server;

import com.wuzi.common.AnsiColor;

public class GameBoard {
    public static final int BOARD_SIZE = 15;
    private int[][] board;
    private int lastX = -1;
    private int lastY = -1;

    public GameBoard() {
        reset();
    }

    public void reset() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        lastX = -1;
        lastY = -1;
    }

    // ===== 坐标转换 =====
    // 棋谱坐标 -> 数组坐标 [x, y]
    public static int[] coordToXY(String coord) {
        if (coord == null || coord.length() < 2 || coord.length() > 3) return null;
        coord = coord.toUpperCase();
        char colChar = coord.charAt(0);
        if (colChar < 'A' || colChar > 'O') return null;
        int x = colChar - 'A';
        int row;
        try {
            row = Integer.parseInt(coord.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (row < 1 || row > BOARD_SIZE) return null;
        int y = row - 1;
        return new int[]{x, y};
    }

    // 数组坐标 -> 棋谱坐标
    public static String xyToCoord(int x, int y) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) return "?";
        return (char) ('A' + x) + String.valueOf(y + 1);
    }

    // ===== 落子 =====
    public boolean makeMove(String coord, int color) {
        int[] xy = coordToXY(coord);
        if (xy == null) return false;
        return makeMove(xy[0], xy[1], color);
    }

    public boolean makeMove(int x, int y, int color) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) return false;
        if (board[x][y] != 0) return false;
        board[x][y] = color;
        lastX = x;
        lastY = y;
        return true;
    }

    // ===== 判断胜利 =====
    public boolean checkWin(int x, int y) {
        int color = board[x][y];
        if (color == 0) return false;

        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            int nx = x + dir[0], ny = y + dir[1];
            while (nx >= 0 && nx < BOARD_SIZE && ny >= 0 && ny < BOARD_SIZE && board[nx][ny] == color) {
                count++;
                nx += dir[0];
                ny += dir[1];
            }
            nx = x - dir[0];
            ny = y - dir[1];
            while (nx >= 0 && nx < BOARD_SIZE && ny >= 0 && ny < BOARD_SIZE && board[nx][ny] == color) {
                count++;
                nx -= dir[0];
                ny -= dir[1];
            }
            if (count >= 5) return true;
        }
        return false;
    }

    // ===== 测试访问器 =====
    public int[][] getBoard() {
        return this.board;
    }

    public int getLastX() {
        return lastX;
    }

    public int getLastY() {
        return lastY;
    }

    // ===== 打印棋盘（仅使用AnsiColor现有颜色） =====
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int size = BOARD_SIZE;
        String COL_GAP = "─";

        // ========== 1. 顶部列号（全黄色，A-O无横线） ==========
        sb.append(AnsiColor.YELLOW);
        sb.append("   ");
        for (int col = 0; col < size; col++) {
            char colLabel = (char) ('A' + col);
            sb.append(colLabel).append(" ");
        }
        sb.append(AnsiColor.RESET).append("\n");

        // ========== 2. 棋盘主体（整行永久黄色，无任何变色） ==========
        for (int rowNum = 15; rowNum >= 1; rowNum--) {
            int y = rowNum - 1;
            // ------------ 行号部分（纯黄色，无任何重置） ------------
            sb.append(AnsiColor.YELLOW); // 整行黄色开关，本行不再关闭
            if (rowNum < 10) sb.append(" ");
            sb.append(rowNum).append(" ");

            // ------------ 网格部分（纯黄色，仅落子单元格局部高亮） ------------
            for (int col = 0; col < size; col++) {
                int x = col;
                String cell = "┼"; // 默认网格

                // 1. 显示棋子（直接在黄色背景上叠加棋子颜色）
                if (board[x][y] == 1) {
                    cell = AnsiColor.WHITE + "●" + AnsiColor.YELLOW; // 白棋+恢复黄
                } else if (board[x][y] == 2) {
                    cell = AnsiColor.BLUE + "○" + AnsiColor.YELLOW;   // 蓝棋+恢复黄
                }

                // 2. 最后落子高亮：仅单元格内红背景，全程不关闭整行黄色
                if (x == lastX && y == lastY) {
                    // 红背景仅包裹单元格，前后都保持黄色
                    cell = AnsiColor.BG_RED + cell + AnsiColor.RESET + AnsiColor.YELLOW;
                }

                // 3. 拼接网格+横线（全程黄色）
                sb.append(cell).append(COL_GAP);
            }

            // ------------ 行尾处理（仅换行，不重置黄色，下一行重新开关） ------------
            sb.append(AnsiColor.RESET).append("\n"); // 仅行尾重置，不影响本行显示
        }

        return sb.toString();
    }
}