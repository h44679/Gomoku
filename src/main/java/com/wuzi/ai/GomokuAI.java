// java
package com.wuzi.ai;

import com.wuzi.common.AnsiColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GomokuAI - 使用 Minimax + Alpha-Beta 的五子棋 AI
 * 特性：
 *  - Minimax 搜索，深度为 4（可调整 MAX_DEPTH）
 *  - Alpha-Beta 剪枝
 *  - 启发式候选点生成与排序（近邻限制 + topK）
 *  - 强化评估函数，识别活四、双三、死四等重要棋型
 *
 * 注意：保持 public int[] getNextStep(int[][] board, int aiColor) 不变。
 */
public class GomokuAI {
    // 用于评估的权重（可根据需要微调）
    private static final int SCORE_FIVE = 1_000_000;
    private static final int SCORE_OPEN_FOUR = 50_000;
    private static final int SCORE_DEAD_FOUR = 10_000;
    private static final int SCORE_OPEN_THREE = 4_000;
    private static final int SCORE_DEAD_THREE = 500;
    private static final int SCORE_OPEN_TWO = 200;
    private static final int SCORE_OTHER = 50;

    // 搜索深度
    private static final int MAX_DEPTH = 4;

    // 方向向量：水平、垂直、右下、左下
    private static final int[][] DIRS = {
            {0, 1},
            {1, 0},
            {1, 1},
            {1, -1}
    };

    // 最大候选数：随深度递减，越靠上越多尝试以利于剪枝效果
    private static int topKForDepth(int depthLeft) {
        int d = MAX_DEPTH - depthLeft;
        // depthLeft == MAX_DEPTH -> top 40; depthLeft == 1 -> top 6; depthLeft == 0 -> evaluate only
        if (depthLeft == MAX_DEPTH) return 40;
        if (depthLeft >= 3) return 20;
        if (depthLeft >= 2) return 12;
        if (depthLeft >= 1) return 8;
        return 6;
    }

    /**
     * 返回 AI 最佳落子坐标 {row, col}，若无空位返回 {-1, -1}。
     * board: 二维数组，0 表示空，1/2 表示棋子
     * aiColor: AI 的颜色（1 或 2）
     */
    public int[] getNextStep(int[][] board, int aiColor) {
        if (board == null || board.length == 0) return new int[]{-1, -1};
        int n = board.length;
        int m = board[0].length;
        int oppColor = (aiColor == 1) ? 2 : 1;

        // 生成候选点并排序
        List<Point> candidates = generateCandidates(board);
        if (candidates.isEmpty()) {
            // 初始局面，优先落中心
            return new int[]{n / 2, m / 2};
        }

        // 启发式排序： attack + defend
        for (Point p : candidates) {
            p.score = evaluatePosition(board, p.r, p.c, aiColor) + evaluatePosition(board, p.r, p.c, oppColor);
        }
        Collections.sort(candidates, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return Integer.compare(o2.score, o1.score);
            }
        });

        // 限制候选数以控制分支
        int limit = Math.min(candidates.size(), topKForDepth(MAX_DEPTH));
        candidates = candidates.subList(0, limit);

        int bestR = -1, bestC = -1;
        int bestVal = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // 根节点：逐一尝试候选点（排序后），应用 Minimax
        for (Point p : candidates) {
            if (!inBounds(board, p.r, p.c) || board[p.r][p.c] != 0) continue;
            board[p.r][p.c] = aiColor; // 落子
            int val;
            if (isFiveAt(board, p.r, p.c, aiColor)) {
                val = SCORE_FIVE;
            } else {
                val = minimax(board, MAX_DEPTH - 1, alpha, beta, false, aiColor, oppColor);
            }
            board[p.r][p.c] = 0; // 撤子

            if (val > bestVal) {
                bestVal = val;
                bestR = p.r;
                bestC = p.c;
            }
            alpha = Math.max(alpha, bestVal);
            if (alpha >= beta) {
                // 根节点剪枝
                break;
            }
        }

        if (bestR == -1) {
            // 兜底，返回第一个空位
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (board[i][j] == 0) return new int[]{i, j};
                }
            }
        }

        return new int[]{bestR, bestC};
    }

    /**
     * Minimax 搜索（带 Alpha-Beta）
     * depthLeft: 剩余深度
     * maximizing: 当前节点是否为最大化（AI）
     */
    private int minimax(int[][] board, int depthLeft, int alpha, int beta, boolean maximizing, int aiColor, int oppColor) {
        // 终止条件
        if (depthLeft <= 0) {
            return evaluateBoard(board, aiColor, oppColor);
        }

        // 生成候选点并按启发式排序
        List<Point> candidates = generateCandidates(board);
        if (candidates.isEmpty()) {
            return evaluateBoard(board, aiColor, oppColor);
        }

        // 为当前角色计算启发式分数（对最大化使用 aiColor 的评估，否则使用 oppColor）
        int currentColor = maximizing ? aiColor : oppColor;
        int opponentColor = maximizing ? oppColor : aiColor;
        for (Point p : candidates) {
            p.score = evaluatePosition(board, p.r, p.c, currentColor) + evaluatePosition(board, p.r, p.c, opponentColor);
        }
        Collections.sort(candidates, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return Integer.compare(o2.score, o1.score);
            }
        });

        int limit = Math.min(candidates.size(), topKForDepth(depthLeft));
        candidates = candidates.subList(0, limit);

        if (maximizing) {
            int value = Integer.MIN_VALUE;
            for (Point p : candidates) {
                if (!inBounds(board, p.r, p.c) || board[p.r][p.c] != 0) continue;
                board[p.r][p.c] = currentColor;
                int childVal;
                if (isFiveAt(board, p.r, p.c, currentColor)) {
                    childVal = SCORE_FIVE;
                } else {
                    childVal = minimax(board, depthLeft - 1, alpha, beta, false, aiColor, oppColor);
                }
                board[p.r][p.c] = 0;
                value = Math.max(value, childVal);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) break; // 剪枝
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Point p : candidates) {
                if (!inBounds(board, p.r, p.c) || board[p.r][p.c] != 0) continue;
                board[p.r][p.c] = currentColor;
                int childVal;
                if (isFiveAt(board, p.r, p.c, currentColor)) {
                    childVal = -SCORE_FIVE;
                } else {
                    childVal = minimax(board, depthLeft - 1, alpha, beta, true, aiColor, oppColor);
                }
                board[p.r][p.c] = 0;
                value = Math.min(value, childVal);
                beta = Math.min(beta, value);
                if (alpha >= beta) break; // 剪枝
            }
            return value;
        }
    }

    /**
     * 对当前棋盘做整体评估：AI 得分 - 对手得分
     * 逻辑：遍历所有空点，计算落子后的进攻/防守潜力，累加为 heuristic。
     */
    private int evaluateBoard(int[][] board, int aiColor, int oppColor) {
        int n = board.length;
        int m = (n > 0 ? board[0].length : 0);
        int score = 0;

        // 遍历所有格子，针对每个空位评估对双方的潜力
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < m; c++) {
                if (board[r][c] != 0) continue;
                int a = evaluatePosition(board, r, c, aiColor);
                int b = evaluatePosition(board, r, c, oppColor);
                score += (a - b);
            }
        }

        // 额外：检测当前已有的连子（如果已存在成五则返回非常大值）
        // 遍历所有已有棋子，若发现任意一方已成五，则直接返回极值
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < m; c++) {
                int v = board[r][c];
                if (v == 0) continue;
                if (isFiveAt(board, r, c, v)) {
                    if (v == aiColor) return SCORE_FIVE;
                    else return -SCORE_FIVE;
                }
            }
        }

        return score;
    }

    /**
     * 评估在 (row, col) 放 color 的总得分（四个方向的综合）。
     * 该函数不修改 board，而是假设落子存在，按棋型给出分值。
     *
     * 通过对四个方向的细分打分可以识别活四、双三等组合：例如若在不同方向各形成活三，
     * 则额外提升（双三）以反映其威胁性。
     */
    private int evaluatePosition(int[][] board, int row, int col, int color) {
        int n = board.length;
        int m = board[0].length;
        if (!inBounds(board, row, col)) return 0;
        if (board[row][col] != 0) return 0;

        int totalScore = 0;
        int openThreeCount = 0;
        boolean hasOpenFour = false;

        for (int[] d : DIRS) {
            DirectionPattern dp = analyseDirection(board, row, col, color, d[0], d[1]);
            totalScore += dp.score;
            if (dp.isOpenThree) openThreeCount++;
            if (dp.isOpenFour) hasOpenFour = true;
        }

        // 双三的识别（两个及以上方向有活三）
        if (openThreeCount >= 2) {
            totalScore += SCORE_OPEN_THREE * 2; // 提高双三价值
        }

        // 若存在活四，额外加分（活四极其危险）
        if (hasOpenFour) totalScore += SCORE_OPEN_FOUR / 2;

        return totalScore;
    }

    /**
     * 在单方向上分析落子后的棋型，返回该方向的得分以及是否为活三/活四等信息。
     */
    private DirectionPattern analyseDirection(int[][] board, int row, int col, int color, int dx, int dy) {
        int n = board.length;
        int m = board[0].length;

        int leftCount = 0, rightCount = 0;

        int r = row - dx, c = col - dy;
        while (inBounds(board, r, c) && board[r][c] == color) {
            leftCount++;
            r -= dx;
            c -= dy;
        }
        boolean leftOpen = inBounds(board, r, c) && board[r][c] == 0;

        r = row + dx; c = col + dy;
        while (inBounds(board, r, c) && board[r][c] == color) {
            rightCount++;
            r += dx;
            c += dy;
        }
        boolean rightOpen = inBounds(board, r, c) && board[r][c] == 0;

        int total = leftCount + 1 + rightCount;
        DirectionPattern dp = new DirectionPattern();

        if (total >= 5) {
            dp.score = SCORE_FIVE;
            return dp;
        }
        if (total == 4) {
            if (leftOpen && rightOpen) {
                dp.score = SCORE_OPEN_FOUR;
                dp.isOpenFour = true;
            } else {
                dp.score = SCORE_DEAD_FOUR;
            }
            return dp;
        }
        if (total == 3) {
            if (leftOpen && rightOpen) {
                dp.score = SCORE_OPEN_THREE;
                dp.isOpenThree = true;
            } else if (leftOpen || rightOpen) {
                dp.score = SCORE_DEAD_THREE;
            } else {
                dp.score = 0;
            }
            return dp;
        }
        if (total == 2) {
            if (leftOpen && rightOpen) dp.score = SCORE_OPEN_TWO;
            else if (leftOpen || rightOpen) dp.score = SCORE_OTHER;
            else dp.score = 0;
            return dp;
        }
        if (total == 1) {
            if (leftOpen && rightOpen) dp.score = SCORE_OTHER / 2;
            else if (leftOpen || rightOpen) dp.score = SCORE_OTHER / 4;
            else dp.score = 0;
            return dp;
        }
        dp.score = 0;
        return dp;
    }

    /**
     * 判断在 (r,c) 放 color 是否直接形成五子（用于终止）
     */
    private boolean isFiveAt(int[][] board, int r, int c, int color) {
        if (!inBounds(board, r, c)) return false;
        for (int[] d : DIRS) {
            int cnt = 1;
            int x = r + d[0], y = c + d[1];
            while (inBounds(board, x, y) && board[x][y] == color) {
                cnt++; x += d[0]; y += d[1];
            }
            x = r - d[0]; y = c - d[1];
            while (inBounds(board, x, y) && board[x][y] == color) {
                cnt++; x -= d[0]; y -= d[1];
            }
            if (cnt >= 5) return true;
        }
        return false;
    }

    /**
     * 生成候选点集合：只保留与已有棋子近邻的空位（半径 2），以减少不必要的搜索。
     * 若棋盘几乎为空，则返回中心点作为候选。
     */
    private List<Point> generateCandidates(int[][] board) {
        int n = board.length;
        int m = board[0].length;
        boolean any = false;
        List<Point> list = new ArrayList<Point>();
        boolean[][] mark = new boolean[n][m];

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < m; c++) {
                if (board[r][c] != 0) {
                    any = true;
                    // 半径 2 内的空格作为候选
                    for (int dr = -2; dr <= 2; dr++) {
                        for (int dc = -2; dc <= 2; dc++) {
                            int nr = r + dr, nc = c + dc;
                            if (inBounds(board, nr, nc) && board[nr][nc] == 0 && !mark[nr][nc]) {
                                mark[nr][nc] = true;
                                list.add(new Point(nr, nc));
                            }
                        }
                    }
                }
            }
        }

        if (!any) {
            // 初始空盘，落中心
            list.add(new Point(n / 2, m / 2));
        }

        return list;
    }

    /**
     * 安全边界判断
     */
    private boolean inBounds(int[][] board, int r, int c) {
        if (board == null) return false;
        if (r < 0 || r >= board.length) return false;
        if (board.length == 0) return false;
        if (c < 0 || c >= board[0].length) return false;
        return true;
    }

    // 简单的点类用于候选点排序
    private static class Point {
        int r, c;
        int score;
        Point(int r, int c) { this.r = r; this.c = c; this.score = 0; }
    }

    // 单方向分析结果
    private static class DirectionPattern {
        int score = 0;
        boolean isOpenThree = false;
        boolean isOpenFour = false;
    }


    /**
     * 渲染棋盘为彩色字符串（和人人对战风格一致）
     * @param board 当前棋盘（0 空，1 白棋，2 蓝棋）
     * @param lastX 最近落子的横坐标
     * @param lastY 最近落子的纵坐标
     * @return 彩色棋盘字符串
     */
    public String boardToString(int[][] board, int lastX, int lastY) {
        if (board == null || board.length == 0) return "";
        int size = board.length;
        StringBuilder sb = new StringBuilder();
        String COL_GAP = "─";

        // ===== 顶部列号 =====
        sb.append(AnsiColor.YELLOW).append("   ");
        for (int col = 0; col < size; col++) {
            char colLabel = (char) ('A' + col);
            sb.append(colLabel).append(" ");
        }
        sb.append(AnsiColor.RESET).append("\n");

        // ===== 棋盘主体 =====
        for (int rowNum = size; rowNum >= 1; rowNum--) {
            int y = rowNum - 1;
            sb.append(AnsiColor.YELLOW);
            if (rowNum < 10) sb.append(" ");
            sb.append(rowNum).append(" ");

            for (int col = 0; col < size; col++) {
                int x = col;
                String cell = "┼"; // 默认网格

                // 棋子显示
                if (board[x][y] == 1) cell = AnsiColor.WHITE + "●" + AnsiColor.YELLOW;
                else if (board[x][y] == 2) cell = AnsiColor.BLUE + "○" + AnsiColor.YELLOW;

                // 最新落子高亮
                if (x == lastX && y == lastY) {
                    cell = AnsiColor.BG_RED + cell + AnsiColor.RESET + AnsiColor.YELLOW;
                }

                sb.append(cell).append(COL_GAP);
            }

            sb.append(AnsiColor.RESET).append("\n"); // 每行末尾重置颜色
        }

        return sb.toString();
    }

}
