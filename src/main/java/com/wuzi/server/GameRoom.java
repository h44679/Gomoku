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

    public synchronized boolean addPlayer(Player player) {
        if (playerCount.get() >= 2) return false;

        if (player1 == null) {
            player1 = player;
            player.setColor("black");
        } else {
            player2 = player;
            player.setColor("white");
        }

        playerCount.incrementAndGet();
        player.setCurrentRoom(this);
        ServerLogger.info("玩家[" + player.getName() + "]加入房间" + roomId);

        if (playerCount.get() == 2) {
            sendMessageToAll("房间已满2人！");
            sendMessageToAll(AnsiColor.color(player1.getName() + "(黑棋) | " + player2.getName() + "(白棋)", AnsiColor.BLUE));
        }
        return true;
    }

    public synchronized void removePlayer(Player player) {
        if (player == null) return;

        if (player1 == player) player1 = null;
        else if (player2 == player) player2 = null;
        else return;

        playerCount.decrementAndGet();
        player.setCurrentRoom(null);
        ServerLogger.info("玩家[" + player.getName() + "]离开房间" + roomId);

        if (playerCount.get() == 0) {
            reset();
        }
    }

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
    }

    public synchronized String makeMove(String xStr, String yStr, String color, Player player) {
        if (!isGameStarted) return AnsiColor.color("游戏未开始！", AnsiColor.RED);
        if (isGameOver) return AnsiColor.color("游戏已结束！", AnsiColor.RED);
        if (!currentTurnColor.equals(color)) return AnsiColor.color("非" + color + "回合！", AnsiColor.RED);
        if (!player.getColor().equals(color)) return AnsiColor.color("你不是" + color + "方！", AnsiColor.RED);

        int x = GameBoard.coordToNum(xStr);
        int y = GameBoard.coordToNum(yStr);
        if (x < 0 || x >= GameBoard.BOARD_SIZE || y < 0 || y >= GameBoard.BOARD_SIZE) {
            return AnsiColor.color("无效坐标！请输入0-E之间的坐标（如7、A）", AnsiColor.RED);
        }

        int colorCode = color.equals("black") ? 1 : 2;
        boolean success = board.makeMove(x, y, colorCode);
        if (!success) {
            return AnsiColor.color("落子失败！该位置已有棋子", AnsiColor.RED);
        }

        if (board.checkWin(x, y)) {
            isGameOver = true;
            String winMsg = AnsiColor.color("恭喜" + player.getName() + "(" + color + ")获胜！", AnsiColor.GREEN);
            sendMessageToAll(winMsg);
            sendMessageToAll(board.toString());
            return winMsg;
        }

        currentTurnColor = currentTurnColor.equals("black") ? "white" : "black";
        String msg = AnsiColor.color("落子成功！当前回合：" + currentTurnColor, AnsiColor.GREEN);
        sendMessageToAll(msg);
        sendMessageToAll(board.toString());
        return msg;
    }

    // 兜底重载：兼容int坐标调用
    public synchronized String makeMove(int x, int y, String color, Player player) {
        return makeMove(String.valueOf(x), String.valueOf(y), color, player);
    }

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
    }

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
}