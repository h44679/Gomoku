package com.wuzi.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoomLogicTest - 房间管理逻辑测试（简化版）
 * 使用真实的Player对象，避免Mockito兼容性问题
 */
@DisplayName("房间管理逻辑测试")
class RoomLogicTest {

    private GameRoom gameRoom;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        gameRoom = new GameRoom(1, true);
        gameRoom.setTestMode(true);

        // 使用真实的Player对象，socket为null表示测试模式
        player1 = new Player("玩家A", null, null);
        player2 = new Player("玩家B", null, null);
    }

    // ==================== 基础初始化测试 ====================

    @Test
    @DisplayName("房间初始化")
    void testRoomInitialization() {
        assertEquals(1, gameRoom.getRoomId());
        assertEquals(0, gameRoom.getPlayerCount());
        assertFalse(gameRoom.isGameStarted());
    }

    @Test
    @DisplayName("房间允许进入")
    void testRoomCanEnter() {
        assertTrue(gameRoom.canEnter());
        assertFalse(gameRoom.isLocked());
    }

    // ==================== 玩家加入测试 ====================

    @Test
    @DisplayName("第一个玩家加入")
    void testFirstPlayerJoin() {
        boolean result = gameRoom.addPlayer(player1);

        assertTrue(result);
        assertEquals(1, gameRoom.getPlayerCount());
        assertEquals(player1, gameRoom.getPlayer1());
        assertNull(gameRoom.getPlayer2());
        assertEquals("black", player1.getColor());
    }

    @Test
    @DisplayName("第二个玩家加入")
    void testSecondPlayerJoin() {
        gameRoom.addPlayer(player1);
        boolean result = gameRoom.addPlayer(player2);

        assertTrue(result);
        assertEquals(2, gameRoom.getPlayerCount());
        assertEquals(player2, gameRoom.getPlayer2());
        assertEquals("white", player2.getColor());
    }

    @Test
    @DisplayName("房间满员拒绝第三个玩家")
    void testThirdPlayerRejected() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        Player player3 = new Player("玩家C", null, null);
        boolean result = gameRoom.addPlayer(player3);

        assertFalse(result);
        assertEquals(2, gameRoom.getPlayerCount());
    }

    // ==================== 游戏启动测试 ====================

    @Test
    @DisplayName("单个玩家准备不启动游戏")
    void testSinglePlayerReadyNotStartGame() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        gameRoom.playerReady(player1);
        assertFalse(gameRoom.isGameStarted());
    }

    @Test
    @DisplayName("两个玩家准备启动游戏")
    void testBothPlayersReadyStartGame() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);

        assertTrue(gameRoom.isGameStarted());
    }

    @Test
    @DisplayName("游戏启动时黑棋先手")
    void testBlackPlayerStartsFirst() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);
        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);

        assertEquals("black", gameRoom.getCurrentTurnColor());
    }

    @Test
    @DisplayName("游戏启动时棋盘重置")
    void testBoardResetOnGameStart() {
        GameBoard board = gameRoom.getBoard();
        board.makeMove("H8", 1);
        assertEquals(1, board.getBoard()[7][7]);

        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);
        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);

        assertEquals(0, board.getBoard()[7][7]);
    }

    // ==================== 玩家离开测试 ====================

    @Test
    @DisplayName("玩家离开房间人数减少")
    void testPlayerRemovalDecreasesCount() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);
        assertEquals(2, gameRoom.getPlayerCount());

        gameRoom.removePlayer(player1);
        assertEquals(1, gameRoom.getPlayerCount());
        assertNull(gameRoom.getPlayer1());
    }

    @Test
    @DisplayName("游戏中玩家离开游戏停止")
    void testGameStopsWhenPlayerLeavesInGame() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);
        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);
        assertTrue(gameRoom.isGameStarted());

        gameRoom.removePlayer(player1);
        assertFalse(gameRoom.isGameStarted());
    }

    @Test
    @DisplayName("最后一个玩家离开房间重置")
    void testRoomResetWhenLastPlayerLeaves() {
        gameRoom.addPlayer(player1);
        gameRoom.removePlayer(player1);

        assertEquals(0, gameRoom.getPlayerCount());
        assertNull(gameRoom.getPlayer1());
    }

    // ==================== 房间锁定测试 ====================

    @Test
    @DisplayName("房间锁定解锁")
    void testRoomLockUnlock() {
        assertFalse(gameRoom.isLocked());

        gameRoom.lockRoom();
        assertTrue(gameRoom.isLocked());
        assertFalse(gameRoom.canEnter());

        gameRoom.unlockRoom();
        assertFalse(gameRoom.isLocked());
        assertTrue(gameRoom.canEnter());
    }

    @Test
    @DisplayName("锁定房间拒绝玩家加入")
    void testLockedRoomRejectsPlayer() {
        gameRoom.lockRoom();

        boolean result = gameRoom.addPlayer(player1);
        assertFalse(result);
        assertEquals(0, gameRoom.getPlayerCount());
    }

    // ==================== 游戏权限测试 ====================

    @Test
    @DisplayName("游戏未开始不能落子")
    void testCannotMoveBeforeGameStart() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        String result = gameRoom.makeMove("H", "8", "black", player1);
        assertNotNull(result);
        assertTrue(result.contains("未开始"));
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("移除null玩家不出错")
    void testRemoveNullPlayerSafely() {
        gameRoom.addPlayer(player1);
        gameRoom.removePlayer(null);

        assertEquals(1, gameRoom.getPlayerCount());
        assertEquals(player1, gameRoom.getPlayer1());
    }

    @Test
    @DisplayName("房间状态完整性")
    void testRoomStateIntegrity() {
        assertEquals(0, gameRoom.getPlayerCount());
        assertNull(gameRoom.getPlayer1());
        assertNull(gameRoom.getPlayer2());

        gameRoom.addPlayer(player1);
        assertEquals(1, gameRoom.getPlayerCount());
        assertEquals(player1, gameRoom.getPlayer1());

        gameRoom.addPlayer(player2);
        assertEquals(2, gameRoom.getPlayerCount());
        assertEquals(player2, gameRoom.getPlayer2());
    }

    // ==================== 游戏流程测试 ====================

    @Test
    @DisplayName("完整游戏流程：初始化-加入-准备-启动")
    void testCompleteGameFlow() {
        // 初始化检查
        assertTrue(gameRoom.canEnter());

        // 玩家加入
        assertTrue(gameRoom.addPlayer(player1));
        assertTrue(gameRoom.addPlayer(player2));
        assertEquals(2, gameRoom.getPlayerCount());

        // 玩家准备
        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);

        // 游戏启动
        assertTrue(gameRoom.isGameStarted());
        assertEquals("black", gameRoom.getCurrentTurnColor());
    }

    @Test
    @DisplayName("玩家颜色分配验证")
    void testPlayerColorAssignment() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        assertEquals("black", player1.getColor());
        assertEquals("white", player2.getColor());
    }

    @Test
    @DisplayName("房间状态转换")
    void testRoomStateTransitions() {
        // 初始未锁定
        assertFalse(gameRoom.isLocked());

        // 加入玩家后仍未锁定
        gameRoom.addPlayer(player1);
        assertFalse(gameRoom.isLocked());

        // 手动锁定
        gameRoom.lockRoom();
        assertTrue(gameRoom.isLocked());

        // 解锁后恢复
        gameRoom.unlockRoom();
        assertFalse(gameRoom.isLocked());
    }

    @Test
    @DisplayName("玩家准备状态验证")
    void testPlayerReadyStatus() {
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);

        // 游戏未开始
        assertFalse(gameRoom.isGameStarted());

        // 第一个玩家准备
        gameRoom.playerReady(player1);
        assertFalse(gameRoom.isGameStarted());

        // 第二个玩家准备，游戏启动
        gameRoom.playerReady(player2);
        assertTrue(gameRoom.isGameStarted());
    }

    @Test
    @DisplayName("棋盘独立性验证")
    void testBoardIndependence() {
        GameBoard board1 = gameRoom.getBoard();

        // 修改棋盘
        board1.makeMove("H8", 1);
        assertEquals(1, board1.getBoard()[7][7]);

        // 游戏启动时棋盘重置
        gameRoom.addPlayer(player1);
        gameRoom.addPlayer(player2);
        gameRoom.playerReady(player1);
        gameRoom.playerReady(player2);

        // 棋盘应该被清空
        GameBoard board2 = gameRoom.getBoard();
        assertEquals(0, board2.getBoard()[7][7]);
        assertTrue(board1 == board2); // 同一个对象被重置
    }

    @Test
    @DisplayName("多次离开不出错")
    void testMultipleRemovalSafety() {
        gameRoom.addPlayer(player1);
        gameRoom.removePlayer(player1);

        // 再次移除同一玩家不出错
        gameRoom.removePlayer(player1);
        assertEquals(0, gameRoom.getPlayerCount());

        // 移除不存在的玩家也不出错
        gameRoom.removePlayer(player2);
        assertEquals(0, gameRoom.getPlayerCount());
    }
}

