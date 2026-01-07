package com.wuzi.server;

import com.wuzi.common.AnsiColor;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRoom {
    private int roomId;
    private GameBoard board;
    private Player player1;
    private Player player2;
    private AtomicInteger playerCount;
    private boolean isGameStarted;
    private boolean isGameOver;
    private String currentTurnColor;
    private boolean isTestMode;
    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private boolean isLocked = false; // 房间锁定标志

    // 【保留功能】锁房（AI对战/单人占用）
    public synchronized void lockRoom() {
        this.isLocked = true;
    }

    // 【保留功能】解锁房间
    public synchronized void unlockRoom() {
        this.isLocked = false;
    }

    // 【保留功能】判断是否可进入
    public synchronized boolean canEnter() {
        return !isLocked && playerCount.get() < 2;
    }

    public GameRoom(int roomId) {
        this(roomId, false);
    }

    public GameRoom(int roomId, boolean isTestMode) {
        this.roomId = roomId;
        this.board = new GameBoard();
        this.playerCount = new AtomicInteger(0);
        this.isGameStarted = false;
        this.isGameOver = false;
        this.currentTurnColor = "black";
        this.isTestMode = isTestMode;
    }

    /**
     * 添加玩家到房间
     */
    public synchronized boolean addPlayer(Player player) {
        if (!canEnter()) {
            player.sendMessage(AnsiColor.color("该房间正在进行游戏或已被锁定，无法加入！", AnsiColor.RED));
            return false;
        }

        if (player1 == null) {
            player1 = player;
            player.setColor("black");
        } else if (player2 == null) {
            player2 = player;
            player.setColor("white");
        } else {
            player.sendMessage(AnsiColor.color("房间已满，无法加入", AnsiColor.RED));
            return false;
        }

        playerCount.incrementAndGet();
        player.setCurrentRoom(this);
        ServerLogger.info("玩家[" + player.getName() + "]加入房间" + roomId);

        player.sendMessage(AnsiColor.color("成功加入房间 " + roomId, AnsiColor.GREEN));

        // 房间满2人时提示准备
        if (playerCount.get() == 2) {
            sendMessageToAll(AnsiColor.color("房间已满，人员齐备！输入“start”准备开始游戏", AnsiColor.BLUE));
        }
        return true;
    }

    /**
     * 移除玩家（离开/断线）
     */
    public synchronized void removePlayer(Player player) {
        if (player == null) return;

        boolean wasPlayer1 = (player == player1);
        if (wasPlayer1) player1 = null;
        else if (player == player2) player2 = null;
        else return; // 玩家不在该房间

        playerCount.decrementAndGet();
        player.setCurrentRoom(null);
        ServerLogger.info("玩家[" + player.getName() + "]离开房间" + roomId);

        if (wasPlayer1) player1Ready = false;
        else player2Ready = false;

        // 【逻辑修复】如果游戏进行中有人离开，强制结束
        if (isGameStarted) {
            isGameStarted = false;
            isGameOver = false;
            board.reset();

            Player opponent = (wasPlayer1 ? player2 : player1);
            if (opponent != null) {
                opponent.sendMessage(AnsiColor.color("对手已离开，当前对局自动结束", AnsiColor.RED));
                // 重置对手状态，允许他继续等待
                if (wasPlayer1) player2Ready = false;
                else player1Ready = false;
            }
        }

        // 房间空时完全重置
        if (playerCount.get() == 0) {
            reset();
        }
    }

    /**
     * 开始游戏
     */
    public synchronized void startGame() {
        if (playerCount.get() < 2) {
            sendMessageToAll(AnsiColor.color("人数不足，无法开始！", AnsiColor.RED));
            return;
        }
        if (isGameStarted) return;

        isGameStarted = true;
        isGameOver = false;
        board.reset();
        sendMessageToAll(AnsiColor.color("游戏开始！黑棋先落子", AnsiColor.GREEN));
        sendMessageToAll(board.toString());
        ServerLogger.success("房间[" + roomId + "]游戏开始");
    }

    /**
     * 落子逻辑：严格遵循 put X Y 格式解析
     */
    public synchronized String makeMove(String p1, String p2, String color, Player player) {
        if (!isGameStarted) return AnsiColor.color("游戏未开始！", AnsiColor.RED);
        if (isGameOver) return AnsiColor.color("游戏已结束！", AnsiColor.RED);
        if (!currentTurnColor.equals(color)) return AnsiColor.color("非" + color + "回合！", AnsiColor.RED);

        // 严格按照 p1+p2 拼接解析，不进行反转尝试
        int[] xy = GameBoard.coordToXY(p1 + p2);

        if (xy == null) {
            return AnsiColor.color("无效坐标！正确格式例：put A 7", AnsiColor.RED);
        }

        int x = xy[0], y = xy[1];
        int colorCode = color.equals("black") ? 1 : 2;

        boolean success = board.makeMove(x, y, colorCode);
        if (!success) return AnsiColor.color("落子失败！该位置已有棋子", AnsiColor.RED);

        if (board.checkWin(x, y)) {
            isGameOver = true;
            String winMsg = "恭喜 " + player.getName() + " (" + color + ") 获胜！";
            ServerLogger.success("房间[" + roomId + "]游戏结束，胜利玩家：" + player.getName());
            return winMsg;
        }

        // 切换回合
        currentTurnColor = currentTurnColor.equals("black") ? "white" : "black";
        return "落子成功！当前回合：" + currentTurnColor;
    }

    /**
     * 玩家准备
     */
    public synchronized void playerReady(Player player) {
        if (player == player1) player1Ready = true;
        else if (player == player2) player2Ready = true;
        else return;

        player.sendMessage(AnsiColor.color("你已准备！", AnsiColor.GREEN));

        // 通知对手（如果对手存在）
        Player opponent = (player == player1) ? player2 : player1;
        if (opponent != null) {
            opponent.sendMessage(AnsiColor.color("对手已准备", AnsiColor.YELLOW));
        }

        if (player1Ready && player2Ready) {
            startGame();
        } else {
            player.sendMessage(AnsiColor.color("等待对手准备...", AnsiColor.BLUE));
        }
    }

    /**
     * 【安全修复】增加 null 判断，防止服务器崩溃
     */
    private void sendMessageToAll(String msg) {
        if (player1 != null) player1.sendMessage(msg);
        if (player2 != null && !isTestMode) player2.sendMessage(msg);
    }

    private void reset() {
        board.reset();
        isGameStarted = false;
        isGameOver = false;
        currentTurnColor = "black";
        player1 = null;
        player2 = null;
        playerCount.set(0);
        player1Ready = false;
        player2Ready = false;
        isLocked = false;
    }

    // Getters 保留
    public int getRoomId() { return roomId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public int getPlayerCount() { return playerCount.get(); }
    public boolean isGameStarted() { return isGameStarted; }
    public boolean isGameOver() { return isGameOver; }
    public String getCurrentTurnColor() { return currentTurnColor; }
    public GameBoard getBoard() { return board; }
    public boolean isTestMode() { return isTestMode; }
    public void setTestMode(boolean testMode) { this.isTestMode = testMode; }
    public boolean isLocked() { return isLocked; }
}