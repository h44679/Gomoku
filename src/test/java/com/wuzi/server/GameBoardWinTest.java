package com.wuzi.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameBoardWinTest - 胜负逻辑测试
 * 验证 checkWin() 方法能正确识别五子连珠的四个方向
 */
@DisplayName("胜负判定逻辑测试")
class GameBoardWinTest {

    private GameBoard gameBoard;

    @BeforeEach
    void setUp() {
        gameBoard = new GameBoard();
    }

    @Test
    @DisplayName("水平方向五子连珠判胜")
    void testCheckWinHorizontal() {
        // 在第8行放置5个黑棋：C8到G8
        gameBoard.makeMove(2, 7, 1);  // C8
        gameBoard.makeMove(3, 7, 1);  // D8
        gameBoard.makeMove(4, 7, 1);  // E8
        gameBoard.makeMove(5, 7, 1);  // F8
        gameBoard.makeMove(6, 7, 1);  // G8

        // 检查中间的棋子(E8)是否被判定为获胜
        assertTrue(gameBoard.checkWin(4, 7), "水平五子连珠应判定为获胜");
    }

    @Test
    @DisplayName("竖直方向五子连珠判胜")
    void testCheckWinVertical() {
        // 在第H列放置5个白棋：H4到H8
        gameBoard.makeMove(7, 3, 2);  // H4
        gameBoard.makeMove(7, 4, 2);  // H5
        gameBoard.makeMove(7, 5, 2);  // H6
        gameBoard.makeMove(7, 6, 2);  // H7
        gameBoard.makeMove(7, 7, 2);  // H8

        // 检查中间的棋子(H6)是否被判定为获胜
        assertTrue(gameBoard.checkWin(7, 5), "竖直五子连珠应判定为获胜");
    }

    @Test
    @DisplayName("右下方向对角线五子连珠判胜")
    void testCheckWinDiagonalRightDown() {
        // 从C3到G7的右下对角线（斜率为正）
        gameBoard.makeMove(2, 2, 1);  // C3
        gameBoard.makeMove(3, 3, 1);  // D4
        gameBoard.makeMove(4, 4, 1);  // E5
        gameBoard.makeMove(5, 5, 1);  // F6
        gameBoard.makeMove(6, 6, 1);  // G7

        // 检查中间的棋子(E5)是否被判定为获胜
        assertTrue(gameBoard.checkWin(4, 4), "右下对角线五子连珠应判定为获胜");
    }

    @Test
    @DisplayName("右上方向对角线五子连珠判胜")
    void testCheckWinDiagonalRightUp() {
        // 从C11到G7的右上对角线（斜率为负）
        gameBoard.makeMove(2, 10, 2);  // C11
        gameBoard.makeMove(3, 9, 2);   // D10
        gameBoard.makeMove(4, 8, 2);   // E9
        gameBoard.makeMove(5, 7, 2);   // F8
        gameBoard.makeMove(6, 6, 2);   // G7

        // 检查中间的棋子(E9)是否被判定为获胜
        assertTrue(gameBoard.checkWin(4, 8), "右上对角线五子连珠应判定为获胜");
    }

    @Test
    @DisplayName("四子连珠不判胜")
    void testCheckWinFourNotWin() {
        // 只放置4个棋子
        gameBoard.makeMove(2, 7, 1);  // C8
        gameBoard.makeMove(3, 7, 1);  // D8
        gameBoard.makeMove(4, 7, 1);  // E8
        gameBoard.makeMove(5, 7, 1);  // F8

        // 四子连珠不应判定为获胜
        assertFalse(gameBoard.checkWin(4, 7), "四子连珠不应判定为获胜");
    }

    @Test
    @DisplayName("六子连珠仍判胜")
    void testCheckWinSixIsWin() {
        // 放置6个棋子
        gameBoard.makeMove(2, 7, 1);  // C8
        gameBoard.makeMove(3, 7, 1);  // D8
        gameBoard.makeMove(4, 7, 1);  // E8
        gameBoard.makeMove(5, 7, 1);  // F8
        gameBoard.makeMove(6, 7, 1);  // G8
        gameBoard.makeMove(7, 7, 1);  // H8

        // 六子连珠应判定为获胜
        assertTrue(gameBoard.checkWin(4, 7), "六子连珠应判定为获胜");
    }

    @Test
    @DisplayName("混合颜色不判胜")
    void testCheckWinMixedColorNotWin() {
        // 在第8行放置：白-白-黑-白-白（混合颜色）
        gameBoard.makeMove(2, 7, 1);  // C8 白
        gameBoard.makeMove(3, 7, 1);  // D8 白
        gameBoard.makeMove(4, 7, 2);  // E8 黑（断开连珠）
        gameBoard.makeMove(5, 7, 1);  // F8 白
        gameBoard.makeMove(6, 7, 1);  // G8 白

        // 混合颜色不应判定为五子连珠
        assertFalse(gameBoard.checkWin(3, 7), "混合颜色不应判定为五子连珠");
    }

    @Test
    @DisplayName("空位检查返回false")
    void testCheckWinEmptyPosition() {
        // 在空位检查
        assertFalse(gameBoard.checkWin(7, 7), "空位检查应返回false");
    }

    @Test
    @DisplayName("边界处水平五子连珠判胜")
    void testCheckWinBoundaryHorizontal() {
        // 在第1行（y=0）放置5个黑棋：K1到O1
        gameBoard.makeMove(10, 0, 1);  // K1
        gameBoard.makeMove(11, 0, 1);  // L1
        gameBoard.makeMove(12, 0, 1);  // M1
        gameBoard.makeMove(13, 0, 1);  // N1
        gameBoard.makeMove(14, 0, 1);  // O1

        // 检查是否在边界处正确判胜
        assertTrue(gameBoard.checkWin(12, 0), "边界处五子连珠应判定为获胜");
    }

    @Test
    @DisplayName("边界处竖直五子连珠判胜")
    void testCheckWinBoundaryVertical() {
        // 在第A列（x=0）放置5个白棋：A1到A5
        gameBoard.makeMove(0, 0, 2);  // A1
        gameBoard.makeMove(0, 1, 2);  // A2
        gameBoard.makeMove(0, 2, 2);  // A3
        gameBoard.makeMove(0, 3, 2);  // A4
        gameBoard.makeMove(0, 4, 2);  // A5

        // 检查是否在边界处正确判胜
        assertTrue(gameBoard.checkWin(0, 2), "边界处五子连珠应判定为获胜");
    }
}

