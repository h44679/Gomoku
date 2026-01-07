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
    private boolean isTestMode;

    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private boolean isLocked = false; // 房间锁定标志
    private String currentTurnColor;

    // 新增：玩家再次请求状态
    private boolean player1WantsAgain = false;
    private boolean player2WantsAgain = false;

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

    // =================== 基础房间管理 ===================
    public synchronized void lockRoom() { this.isLocked = true; }
    public synchronized void unlockRoom() { this.isLocked = false; }
    public synchronized boolean canEnter() { return !isLocked && playerCount.get() < 2; }

    public synchronized boolean addPlayer(Player player) {
        if (!canEnter()) {
            player.sendMessage(AnsiColor.color("该房间正在进行游戏或已被锁定，无法加入！", AnsiColor.RED));
            return false;
        }
        if (player1 == null) { player1 = player; player.setColor("black"); }
        else if (player2 == null) { player2 = player; player.setColor("white"); }
        else { player.sendMessage(AnsiColor.color("房间已满，无法加入", AnsiColor.RED)); return false; }

        playerCount.incrementAndGet();
        player.setCurrentRoom(this);
        ServerLogger.info("玩家[" + player.getName() + "]加入房间" + roomId);

        player.sendMessage(AnsiColor.color("成功加入房间 " + roomId, AnsiColor.GREEN));
        if (playerCount.get() == 2)
            sendMessageToAll(AnsiColor.color("房间已满，人员齐备！输入“start”准备开始游戏", AnsiColor.BLUE));
        return true;
    }

    public synchronized void removePlayer(Player player) {
        if (player == null) return;

        boolean wasPlayer1 = (player == player1);
        if (wasPlayer1) player1 = null;
        else if (player == player2) player2 = null;
        else return;

        playerCount.decrementAndGet();
        player.setCurrentRoom(null);
        ServerLogger.info("玩家[" + player.getName() + "]离开房间" + roomId);

        if (wasPlayer1) player1Ready = false;
        else player2Ready = false;

        if (isGameStarted) {
            isGameStarted = false;
            isGameOver = false;
            board.reset();
            Player opponent = (wasPlayer1 ? player2 : player1);
            if (opponent != null) {
                opponent.sendMessage(AnsiColor.color("对手已离开，当前对局自动结束", AnsiColor.RED));
                if (wasPlayer1) player2Ready = false;
                else player1Ready = false;
            }
        }

        if (playerCount.get() == 0) reset();
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
        player1WantsAgain = false;
        player2WantsAgain = false;
        ServerLogger.info("房间[" + roomId + "]已逻辑销毁（重置）");
    }

    // =================== 游戏逻辑 ===================
    public synchronized void startGame() {
        if (playerCount.get() < 2) { sendMessageToAll(AnsiColor.color("人数不足，无法开始！", AnsiColor.RED)); return; }
        if (isGameStarted) return;

        isGameStarted = true;
        isGameOver = false;
        board.reset();
        sendMessageToAll(AnsiColor.color("游戏开始！黑棋先落子", AnsiColor.GREEN));
        sendMessageToAll(board.toString());
        ServerLogger.success("房间[" + roomId + "]游戏开始");
    }

    public synchronized String makeMove(String p1, String p2, String color, Player player) {
        if (!isGameStarted) return AnsiColor.color("游戏未开始！", AnsiColor.RED);
        if (isGameOver) return AnsiColor.color("游戏已结束！", AnsiColor.RED);
        if (!currentTurnColor.equals(color)) return AnsiColor.color("非" + color + "回合！", AnsiColor.RED);

        int[] xy = GameBoard.coordToXY(p1 + p2);
        if (xy == null) return AnsiColor.color("无效坐标！正确格式例：put A 7", AnsiColor.RED);

        int x = xy[0], y = xy[1];
        int colorCode = color.equals("black") ? 1 : 2;

        boolean success = board.makeMove(x, y, colorCode);
        if (!success) return AnsiColor.color("落子失败！该位置已有棋子", AnsiColor.RED);

        if (board.checkWin(x, y)) {
            isGameOver = true;
            ServerLogger.success("房间[" + roomId + "]游戏结束，胜利玩家：" + player.getName());
            return "恭喜 " + player.getName() + " (" + color + ") 获胜！";
        }

        currentTurnColor = currentTurnColor.equals("black") ? "white" : "black";
        return "落子成功！当前回合：" + currentTurnColor;
    }

    // =================== 玩家准备 ===================
    public synchronized void playerReady(Player player) {
        if (player == player1) player1Ready = true;
        else if (player == player2) player2Ready = true;
        else return;

        player.sendMessage(AnsiColor.color("你已准备！", AnsiColor.GREEN));
        Player opponent = (player == player1) ? player2 : player1;
        if (opponent != null) opponent.sendMessage(AnsiColor.color("对手已准备", AnsiColor.YELLOW));

        if (player1Ready && player2Ready) {
            assignColorsRandomly();
            startGame();
        } else {
            player.sendMessage(AnsiColor.color("等待对手准备...", AnsiColor.BLUE));
        }
    }

    private void assignColorsRandomly() {
        if (Math.random() < 0.5) { player1.setColor("black"); player2.setColor("white"); }
        else { player1.setColor("white"); player2.setColor("black"); }

        player1.sendMessage(AnsiColor.color("你执" + player1.getColor() + "棋，黑棋先下", AnsiColor.BLUE));
        player2.sendMessage(AnsiColor.color("你执" + player2.getColor() + "棋，黑棋先下", AnsiColor.BLUE));

        currentTurnColor = "black";
    }

    // =================== 再来一局 ===================
    public synchronized void requestAgain(Player player) {
        if (player == player1) player1WantsAgain = true;
        else if (player == player2) player2WantsAgain = true;
        else return;

        Player opponent = (player == player1) ? player2 : player1;

        if (opponent == null) { resetGame(); return; }

        if (player1WantsAgain && player2WantsAgain) {
            player1WantsAgain = false;
            player2WantsAgain = false;
            resetGame();
        } else {
            player.sendMessage(AnsiColor.color("你已请求再来一局，等待对手选择", AnsiColor.BLUE));
            opponent.sendMessage(AnsiColor.color("对手请求再来一局，请输入 again 接受或 leave 退出", AnsiColor.BLUE));
        }
    }

    public synchronized void resetGame() {
        board.reset();
        isGameStarted = false;
        isGameOver = false;
        currentTurnColor = "black";
        player1Ready = false;
        player2Ready = false;
        assignColorsRandomly();

        // 发送棋盘给双方
        sendMessageToAll(board.toString());
        sendMessageToAll(AnsiColor.color("新的一局已开始，请开始落子！", AnsiColor.BLUE));
    }

    private void sendMessageToAll(String msg) {
        if (player1 != null) player1.sendMessage(msg);
        if (player2 != null && !isTestMode) player2.sendMessage(msg);
    }

    // ========== Getters ==========
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
