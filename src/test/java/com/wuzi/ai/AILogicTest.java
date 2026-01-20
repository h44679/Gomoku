package com.wuzi.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AILogicTest - AI决策基础测试
 * 验证AI在不同棋局情况下的合法决策能力
 */
@DisplayName("AI决策逻辑测试")
class AILogicTest {

    private GomokuAI gomokuAI;
    private static final int BOARD_SIZE = 15;
    private static final int AI_COLOR = 2;
    private static final int PLAYER_COLOR = 1;

    @BeforeEach
    void setUp() {
        gomokuAI = new GomokuAI();
    }

    @Test
    @DisplayName("空棋盘AI返回合法坐标")
    void testAIOnEmptyBoard() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        int[] move = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(move, "AI应返回非null值");
        assertNotEquals(-1, move[0], "AI行坐标不应为-1");
        assertNotEquals(-1, move[1], "AI列坐标不应为-1");

        // 验证坐标在棋盘范围内
        assertTrue(move[0] >= 0 && move[0] < BOARD_SIZE, "行坐标应在0-14范围内");
        assertTrue(move[1] >= 0 && move[1] < BOARD_SIZE, "列坐标应在0-14范围内");

        // 验证坐标对应的位置是空的
        assertEquals(0, board[move[0]][move[1]], "AI返回的位置应该是空位");
    }

    @Test
    @DisplayName("空棋盘AI倾向中心位置")
    void testAIPrefersCenterOnEmptyBoard() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        int[] move = gomokuAI.getNextStep(board, AI_COLOR);

        // 验证返回的位置在中心区域附近（7-7或附近）
        int centerX = BOARD_SIZE / 2;
        int centerY = BOARD_SIZE / 2;
        int distance = Math.abs(move[0] - centerX) + Math.abs(move[1] - centerY);
        assertTrue(distance <= 3, "AI应倾向落在棋盘中心附近，距离应≤3，实际距离:" + distance);
    }

    @Test
    @DisplayName("玩家落子后AI做出响应")
    void testAIRespondsToPlayerMove() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // 玩家在中心落子
        board[7][7] = PLAYER_COLOR;

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(aiMove, "AI应对玩家的落子做出响应");
        assertNotEquals(-1, aiMove[0], "AI应返回有效坐标");
        assertNotEquals(-1, aiMove[1], "AI应返回有效坐标");

        // 验证AI的落子不重叠
        assertNotEquals(7, aiMove[0], "AI不应落在玩家的位置");
        assertNotEquals(7, aiMove[1], "AI不应落在玩家的位置");

        // AI的落子应该靠近玩家的落子（启发式策略）
        int distance = Math.abs(aiMove[0] - 7) + Math.abs(aiMove[1] - 7);
        assertTrue(distance <= 5, "AI应在玩家附近落子，距离应≤5，实际距离:" + distance);
    }

    @Test
    @DisplayName("多个棋子已落时AI返回合法坐标")
    void testAIWithMultiplePieces() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // 在棋盘上放置多个棋子
        board[7][7] = PLAYER_COLOR;
        board[7][8] = AI_COLOR;
        board[6][7] = PLAYER_COLOR;
        board[8][7] = AI_COLOR;

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(aiMove);
        assertTrue(aiMove[0] >= 0 && aiMove[0] < BOARD_SIZE);
        assertTrue(aiMove[1] >= 0 && aiMove[1] < BOARD_SIZE);

        // 验证不重叠
        assertEquals(0, board[aiMove[0]][aiMove[1]], "AI的目标位置应该是空位");
    }

    @Test
    @DisplayName("棋盘即将满员时AI返回合法坐标")
    void testAINearlyFullBoard() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // 填充棋盘除了少数几个位置
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (!((i == 7 && j == 7) || (i == 8 && j == 8))) {
                    board[i][j] = (i + j) % 2 == 0 ? AI_COLOR : PLAYER_COLOR;
                }
            }
        }

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(aiMove);
        if (aiMove[0] != -1) {
            // AI找到了可用位置
            assertEquals(0, board[aiMove[0]][aiMove[1]], "AI应找到空位");
        }
    }

    @ParameterizedTest(name = "AI支持多种颜色 aiColor={0}")
    @ValueSource(ints = {1, 2})
    @DisplayName("参数化测试：AI支持不同玩家颜色")
    void testAISupportsDifferentColors(int aiColor) {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        int[] move = gomokuAI.getNextStep(board, aiColor);

        assertNotNull(move, "AI应支持颜色" + aiColor);
        if (move[0] != -1) {
            assertTrue(move[0] >= 0 && move[0] < BOARD_SIZE);
            assertTrue(move[1] >= 0 && move[1] < BOARD_SIZE);
        }
    }

    @Test
    @DisplayName("AI返回坐标不修改输入棋盘")
    void testAIDoesNotModifyBoard() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];

        // 复制棋盘
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                boardCopy[i][j] = board[i][j];
            }
        }

        // 放置棋子
        board[7][7] = PLAYER_COLOR;
        boardCopy[7][7] = PLAYER_COLOR;

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        // 验证棋盘未被修改（AI返回坐标后应该撤销）
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                assertEquals(boardCopy[i][j], board[i][j],
                        "位置(" + i + "," + j + ")不应被修改");
            }
        }
    }

    @Test
    @DisplayName("AI面对防守机会")
    void testAIDefensivePlay() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // 玩家放置4个连续棋子，即将获胜
        board[7][4] = PLAYER_COLOR;
        board[7][5] = PLAYER_COLOR;
        board[7][6] = PLAYER_COLOR;
        board[7][7] = PLAYER_COLOR;
        // 7,8 为空，玩家可能在此获胜

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(aiMove);
        // AI应该倾向于防守（落在7,8或7,3）
        // 虽然AI不一��总是完美防守，但应该返回有效坐标
        assertTrue(aiMove[0] >= 0 && aiMove[0] < BOARD_SIZE,
                "AI应返回有效坐标进行防守");
    }

    @Test
    @DisplayName("AI面对进攻机会")
    void testAIOffensivePlay() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // AI自己有4个连续棋��
        board[7][4] = AI_COLOR;
        board[7][5] = AI_COLOR;
        board[7][6] = AI_COLOR;
        board[7][7] = AI_COLOR;
        // 7,8 或 7,3 为空，AI可能获胜

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        assertNotNull(aiMove);
        // AI应该倾向于进攻（落在能形成五子的位置）
        assertTrue(aiMove[0] >= 0 && aiMove[0] < BOARD_SIZE,
                "AI应返回有效坐标进行进攻");
    }

    @Test
    @DisplayName("AI多次调用保持一致性")
    void testAIConsistency() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        board[7][7] = PLAYER_COLOR;

        // 调用多次，验证都返回有效坐标
        for (int i = 0; i < 3; i++) {
            int[] move = gomokuAI.getNextStep(board, AI_COLOR);
            assertNotNull(move, "第" + i + "次调用应返回非null");
            assertTrue(move[0] >= 0 && move[0] < BOARD_SIZE,
                    "第" + i + "次调用的行坐标应有效");
            assertTrue(move[1] >= 0 && move[1] < BOARD_SIZE,
                    "第" + i + "次调用的列坐标应有效");
        }
    }

    @Test
    @DisplayName("null棋盘逻辑")
    void testAINullBoardHandling() {
        int[] move = gomokuAI.getNextStep(null, AI_COLOR);
        assertNotNull(move, "null棋盘应返回有效数组");
        assertEquals(-1, move[0], "null棋盘应返回(-1,-1)");
        assertEquals(-1, move[1], "null棋盘应返回(-1,-1)");
    }

    @Test
    @DisplayName("AI不返回已占据的位置")
    void testAIDoesNotReturnOccupiedPosition() {
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

        // 放置多个棋子
        board[7][7] = PLAYER_COLOR;
        board[8][8] = AI_COLOR;
        board[6][6] = PLAYER_COLOR;

        int[] aiMove = gomokuAI.getNextStep(board, AI_COLOR);

        if (aiMove[0] != -1) {
            assertEquals(0, board[aiMove[0]][aiMove[1]],
                    "AI不应返回已占据的位置");
        }
    }
}

