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
    private boolean isLocked = false; // 新增锁定标志

    // 锁房（AI 对战/单人占用房间时调用）
    public synchronized void lockRoom() {
        this.isLocked = true;
    }

    // 解锁房间（AI 对战结束或没人时调用）
    public synchronized void unlockRoom() {
        this.isLocked = false;
    }

    // 判断是否可进入
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
        // 先告诉玩家加入成功
        player.sendMessage(AnsiColor.color("成功加入房间 " + roomId, AnsiColor.GREEN));
        if (playerCount.get() == 2) {
            // 告诉先加入的玩家对手已进入
            if (player1 != null)
                player1.sendMessage(AnsiColor.color("您的对手已进入房间，输入“start”准备开始游戏", AnsiColor.BLUE));

            // 告诉后加入的玩家房间已满
            if (player2 != null)
                player2.sendMessage(AnsiColor.color("房间已满，输入“start”准备开始游戏", AnsiColor.BLUE));
        }
        return true;
    }

    public synchronized void removePlayer(Player player) {
        if (player == null) return;

        boolean wasPlayer1 = player == player1;
        if (wasPlayer1) player1 = null;
        else if (player == player2) player2 = null;
        else return;

        playerCount.decrementAndGet();
        player.setCurrentRoom(null);
        ServerLogger.info("玩家[" + player.getName() + "]离开房间" + roomId);

        // 重置准备状态
        if (wasPlayer1) player1Ready = false;
        else player2Ready = false;

        // 取消游戏状态
        if (isGameStarted) {
            isGameStarted = false;
            isGameOver = false;
            board.reset();

            Player opponent = (wasPlayer1 ? player2 : player1);
            if (opponent != null) {
                opponent.sendMessage(AnsiColor.color("由于对手已离开，当前对局已取消", AnsiColor.BLUE));
            }
        }

        // ⭐ 彻底重置，防止房间满了
        if (playerCount.get() < 2) {
            isGameStarted = false;
            isGameOver = false;
            currentTurnColor = "black";
        }

        // 如果没人了，再完全清理
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
        // ===== 在这里加日志 =====
        ServerLogger.success("房间[" + roomId + "]游戏开始");

    }

    public synchronized String makeMove(String coord, String color, Player player) {
        if (!isGameStarted) return AnsiColor.color("游戏未开始！", AnsiColor.RED);
        if (isGameOver) return AnsiColor.color("游戏已结束！", AnsiColor.RED);
        if (!currentTurnColor.equals(color)) return AnsiColor.color("非" + color + "回合！", AnsiColor.RED);
        if (!player.getColor().equals(color)) return AnsiColor.color("你不是" + color + "方！", AnsiColor.RED);

        int[] xy = GameBoard.coordToXY(coord);
        if (xy == null) {
            return AnsiColor.color("无效坐标！请输入A-O + 1-15的棋谱坐标（如D4、H8）", AnsiColor.RED);
        }
        int x = xy[0], y = xy[1];
        int colorCode = color.equals("black") ? 1 : 2;

        boolean success = board.makeMove(x, y, colorCode);
        if (!success) return AnsiColor.color("落子失败！该位置已有棋子", AnsiColor.RED);

        if (board.checkWin(x, y)) {
            isGameOver = true;
            String winMsg = AnsiColor.color("恭喜" + player.getName() + "(" + color + ")获胜！", AnsiColor.GREEN);
            sendMessageToAll(winMsg);
            sendMessageToAll(board.toString());
            ServerLogger.success("房间[" + roomId + "]游戏结束，胜利玩家：" + player.getName());
            return winMsg;
        }

        currentTurnColor = currentTurnColor.equals("black") ? "white" : "black";
        String msg = AnsiColor.color("落子成功！当前回合：" + currentTurnColor, AnsiColor.GREEN);
        sendMessageToAll(msg);
        sendMessageToAll(board.toString());
        return msg;
    }

    public synchronized String makeMove(String xStr, String yStr, String color, Player player) {
        return makeMove(xStr + yStr, color, player);
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
        player1Ready = false;
        player2Ready = false;
    }

    public synchronized void playerReady(Player player) {
        if (player == player1) player1Ready = true;
        else if (player == player2) player2Ready = true;
        else return;

        ServerLogger.info("玩家[" + player.getName() + "]已准备");

        if (player1Ready && player2Ready) {
            // 两人都准备，开始游戏
            isGameStarted = true;
            isGameOver = false;
            board.reset();
            player1.sendMessage(AnsiColor.color("游戏开始！你是黑棋，黑手先", AnsiColor.GREEN));
            player2.sendMessage(AnsiColor.color("游戏开始！你是白棋，白手后", AnsiColor.GREEN));

            sendMessageToAll(board.toString());
            ServerLogger.success("房间[" + roomId + "]游戏开始");
        } else {
            // 只准备了一个，等待对手
            player.sendMessage(AnsiColor.color("等待对手准备...", AnsiColor.BLUE));
        }
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
