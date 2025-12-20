package com.wuzi.server;

public class GameBoard {
    public static final int BOARD_SIZE = 15;
    private int[][] board;

    public GameBoard() {
        reset();
    }

    public void reset() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
    }

    public static int coordToNum(String coord) {
        if (coord == null || coord.length() != 1) return -1;
        char c = coord.toUpperCase().charAt(0);
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'E') {
            return 10 + (c - 'A');
        } else {
            return -1;
        }
    }

    public static String numToCoord(int num) {
        if (num >= 0 && num <= 9) {
            return String.valueOf(num);
        } else if (num >= 10 && num <= 14) {
            return String.valueOf((char) ('A' + (num - 10)));
        } else {
            return "?";
        }
    }

    public boolean makeMove(String xStr, String yStr, int color) {
        int x = coordToNum(xStr);
        int y = coordToNum(yStr);
        return makeMove(x, y, color);
    }

    public boolean makeMove(int x, int y, int color) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) return false;
        if (board[x][y] != 0) return false;
        board[x][y] = color;
        return true;
    }

    public boolean checkWin(int x, int y) {
        int color = board[x][y];
        if (color == 0) return false;

        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            int nx = x + dir[0];
            int ny = y + dir[1];
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
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int i = 0; i < BOARD_SIZE; i++) {
            sb.append(numToCoord(i)).append(" ");
        }
        sb.append("\n");

        for (int i = 0; i < BOARD_SIZE; i++) {
            sb.append(numToCoord(i)).append("  ");
            for (int j = 0; j < BOARD_SIZE; j++) {
                String piece = switch (board[i][j]) {
                    case 1 -> "●";
                    case 2 -> "○";
                    default -> "┼";
                };
                sb.append(piece).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public int[][] getBoard() {
        return board;
    }
}