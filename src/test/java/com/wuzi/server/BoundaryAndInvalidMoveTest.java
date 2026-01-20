package com.wuzi.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoundaryAndInvalidMoveTest - 边界与非法落子测试
 * 测试棋盘边界条件和非法操作的处理
 */
@DisplayName("边界与非法落子测试")
class BoundaryAndInvalidMoveTest {

    private GameBoard gameBoard;

    @BeforeEach
    void setUp() {
        gameBoard = new GameBoard();
    }

    // ============ 边界条件测试 ============

    @Test
    @DisplayName("左上角A1落子合法")
    void testMoveTopLeftCorner() {
        boolean result = gameBoard.makeMove("A1", 1);
        assertTrue(result, "A1位置应能成功落子");
        assertEquals(1, gameBoard.getBoard()[0][0], "A1应成功放置黑棋");
    }

    @Test
    @DisplayName("右下角O15落子合法")
    void testMoveBottomRightCorner() {
        boolean result = gameBoard.makeMove("O15", 2);
        assertTrue(result, "O15位置应能成功落子");
        assertEquals(2, gameBoard.getBoard()[14][14], "O15应成功放置白棋");
    }

    @Test
    @DisplayName("左下角A15落子合法")
    void testMoveBottomLeftCorner() {
        boolean result = gameBoard.makeMove("A15", 1);
        assertTrue(result, "A15位置应能成功落子");
        assertEquals(1, gameBoard.getBoard()[0][14], "A15应成功放置黑棋");
    }

    @Test
    @DisplayName("右上角O1落子合法")
    void testMoveTopRightCorner() {
        boolean result = gameBoard.makeMove("O1", 2);
        assertTrue(result, "O1位置应能成功落子");
        assertEquals(2, gameBoard.getBoard()[14][0], "O1应成功放置白棋");
    }

    @Test
    @DisplayName("棋盘边缘H1落子合法")
    void testMoveEdgeH1() {
        boolean result = gameBoard.makeMove("H1", 1);
        assertTrue(result, "H1应能成功落子");
    }

    @Test
    @DisplayName("棋盘边缘O8落子合法")
    void testMoveEdgeO8() {
        boolean result = gameBoard.makeMove("O8", 2);
        assertTrue(result, "O8应能成功落子");
    }

    // ============ 非法落子测试 ============

    @Test
    @DisplayName("重复落子返回false")
    void testRepeatMoveReturnsFalse() {
        // 第一次落子成功
        boolean firstMove = gameBoard.makeMove("H8", 1);
        assertTrue(firstMove, "第一次落子应成功");

        // 第二次在同一位置落子应失败
        boolean secondMove = gameBoard.makeMove("H8", 2);
        assertFalse(secondMove, "��复落子应返回false");

        // 验证棋子颜色未被覆盖
        assertEquals(1, gameBoard.getBoard()[7][7], "棋子应保持原颜色");
    }

    @Test
    @DisplayName("多次重复落子拦截")
    void testMultipleRepeatMovesBlocked() {
        gameBoard.makeMove("H8", 1);

        // 尝试用不同颜色重复落子
        assertFalse(gameBoard.makeMove("H8", 2), "白棋不能落在黑棋位置");
        assertFalse(gameBoard.makeMove("H8", 1), "黑棋不能再落在相同位置");
        assertFalse(gameBoard.makeMove("H8", 2), "任何颜色都不能重复落子");

        assertEquals(1, gameBoard.getBoard()[7][7], "棋子应保持不变");
    }

    @ParameterizedTest
    @DisplayName("越界落子全部返回false")
    @ValueSource(strings = {"A0", "A16", "Z8", "P8", "-1-1"})
    void testOutOfBoundsMoves(String coord) {
        boolean result = gameBoard.makeMove(coord, 1);
        assertFalse(result, coord + " 应被拒绝");
    }

    @Test
    @DisplayName("空字符串落子返回false")
    void testEmptyStringMove() {
        boolean result = gameBoard.makeMove("", 1);
        assertFalse(result, "空字符串应返回false");
    }

    @Test
    @DisplayName("null落子返回false")
    void testNullMove() {
        boolean result = gameBoard.makeMove(null, 1);
        assertFalse(result, "null输入应返回false");
    }

    @Test
    @DisplayName("无效坐标格式返回false")
    void testInvalidCoordFormat() {
        assertFalse(gameBoard.makeMove("H", 1), "缺少行号应返回false");
        assertFalse(gameBoard.makeMove("8", 1), "仅数字应返回false");
        assertFalse(gameBoard.makeMove("HH", 1), "两个字母应返回false");
        assertFalse(gameBoard.makeMove("H8H", 1), "格式错误应返回false");
    }

    // ============ 棋盘状态验证 ============

    @Test
    @DisplayName("非法操作后棋盘状态不变")
    void testBoardStateUnchangedAfterInvalidMove() {
        // 记录初始状态
        int[][] originalBoard = new int[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                originalBoard[i][j] = gameBoard.getBoard()[i][j];
            }
        }

        // 执行非法操作
        gameBoard.makeMove("A0", 1);
        gameBoard.makeMove(null, 2);
        gameBoard.makeMove("Z99", 1);

        // 验证棋盘状态未变
        int[][] currentBoard = gameBoard.getBoard();
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                assertEquals(originalBoard[i][j], currentBoard[i][j],
                        "位置(" + i + "," + j + ")应保持原状");
            }
        }
    }

    @Test
    @DisplayName("合法落子与非法落子混合")
    void testMixedValidAndInvalidMoves() {
        // 合法落子
        assertTrue(gameBoard.makeMove("H8", 1), "H8应落子成功");

        // 非法落子（同位置）
        assertFalse(gameBoard.makeMove("H8", 2), "H8重复落子应失败");

        // 合法落子（不同位置）
        assertTrue(gameBoard.makeMove("H9", 2), "H9应落子成功");

        // 验证棋盘状态
        assertEquals(1, gameBoard.getBoard()[7][7], "H8应为黑棋");
        assertEquals(2, gameBoard.getBoard()[7][8], "H9应为白棋");
    }

    @Test
    @DisplayName("使用数组索引越界落子")
    void testArrayIndexOutOfBounds() {
        // 直接使用数组坐标越界
        assertFalse(gameBoard.makeMove(-1, 7, 1), "负数索引应失败");
        assertFalse(gameBoard.makeMove(15, 7, 1), "超范围索引应失败");
        assertFalse(gameBoard.makeMove(7, -1, 1), "负数行应失败");
        assertFalse(gameBoard.makeMove(7, 15, 1), "超范围行应失败");
    }

    @Test
    @DisplayName("棋盘满员后的落子尝试")
    void testMoveWhenBoardNearlyFull() {
        // 填充大部分棋盘，只留H8为空
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                if (!(i == 7 && j == 7)) {
                    gameBoard.makeMove(i, j, (i + j) % 2 == 0 ? 1 : 2);
                }
            }
        }

        // 在空位落子应成功
        assertTrue(gameBoard.makeMove(7, 7, 1), "空位应能成功落子");

        // 在任何位置重复落子应失败
        assertFalse(gameBoard.makeMove(0, 0, 2), "满员棋盘的任何位置都不能重复落子");
    }

    @Test
    @DisplayName("记录最后落子位置")
    void testLastMoveTracking() {
        gameBoard.makeMove(5, 10, 1);
        assertEquals(5, gameBoard.getLastX(), "lastX应被正确记录");
        assertEquals(10, gameBoard.getLastY(), "lastY应被正确记录");

        // 无效操作不应更新最后位置
        int lastX = gameBoard.getLastX();
        int lastY = gameBoard.getLastY();
        gameBoard.makeMove("Z99", 2);
        assertEquals(lastX, gameBoard.getLastX(), "非法操作不应更新lastX");
        assertEquals(lastY, gameBoard.getLastY(), "非法操作不应更新lastY");
    }
}

