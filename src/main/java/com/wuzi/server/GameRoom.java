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

    // 锁房（AI对战/单人占用）
    public synchronized void lockRoom() {
        this.isLocked = true;
    }

    // 解锁房间
    public synchronized void unlockRoom() {
        this.isLocked = false;
    }

    // 判断是否可进入
    public synchronized boolean canEnter() {
        return !isLocked && playerCount.get() < 2;
    }

    // 构造方法（默认非测试模式）
    public GameRoom(int roomId) {
        this(roomId, false);
    }

    // 构造方法（支持测试模式）
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

        // 分配玩家和颜色
        if (player1 == null) {
            player1 = player;
            player.setColor("black"); // 先手黑棋
        } else if (player2 == null) {
            player2 = player;
            player.setColor("white"); // 后手白棋
        } else {
            player.sendMessage(AnsiColor.color("房间已满，无法加入", AnsiColor.RED));
            return false;
        }

        playerCount.incrementAndGet();
        player.setCurrentRoom(this);
        ServerLogger.info("玩家[" + player.getName() + "]加入房间" + roomId);

        // 提示玩家加入成功
        player.sendMessage(AnsiColor.color("成功加入房间 " + roomId, AnsiColor.GREEN));

        // 房间满2人时提示准备
        if (playerCount.get() == 2) {
            if (player1 != null)
                player1.sendMessage(AnsiColor.color("您的对手已进入房间，输入“start”准备开始游戏", AnsiColor.BLUE));
            if (player2 != null)
                player2.sendMessage(AnsiColor.color("房间已满，输入“start”准备开始游戏", AnsiColor.BLUE));
        }
        return true;
    }

    /**
     * 移除玩家（离开/断线）
     */
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

        // 重置房间状态
        if (playerCount.get() < 2) {
            isGameStarted = false;
            isGameOver = false;
            currentTurnColor = "black";
        }

        // 房间空时完全重置
        if (playerCount.get() == 0) {
            reset();
        }
    }

    /**
     * 开始游戏（主动调用）
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
     * 核心：落子逻辑（3参数，纯文本返回）
     */
    // GameRoom 类的 makeMove 方法（完整错误着色）
    public synchronized String makeMove(String coord, String color, Player player) {
        // 前置校验：所有错误返回都加红色
        if (!isGameStarted) return AnsiColor.color("游戏未开始！", AnsiColor.RED);
        if (isGameOver) return AnsiColor.color("游戏已结束！", AnsiColor.RED);
        if (!currentTurnColor.equals(color)) return AnsiColor.color("非" + color + "回合！", AnsiColor.RED);
        if (!player.getColor().equals(color)) return AnsiColor.color("你不是" + color + "方！", AnsiColor.RED);

        // 解析坐标：无效坐标加红色
        int[] xy = GameBoard.coordToXY(coord);
        if (xy == null) {
            return AnsiColor.color("无效坐标！请输入A-O + 1-15的棋谱坐标（如D4、H8）", AnsiColor.RED);
        }
        int x = xy[0], y = xy[1];
        int colorCode = color.equals("black") ? 1 : 2;

        // 落子失败：加红色
        boolean success = board.makeMove(x, y, colorCode);
        if (!success) return AnsiColor.color("落子失败！该位置已有棋子", AnsiColor.RED);

        // 胜负判断：正常返回（无颜色，由ClientHandler着色）
        if (board.checkWin(x, y)) {
            isGameOver = true;
            String winMsg = "恭喜" + player.getName() + "(" + color + ")获胜！";
            ServerLogger.success("房间[" + roomId + "]游戏结束，胜利玩家：" + player.getName());
            return winMsg;
        }

        // 正常落子：正常返回
        currentTurnColor = currentTurnColor.equals("black") ? "white" : "black";
        return "落子成功！当前回合：" + currentTurnColor;
    }

    /**
     * 重载：4参数落子方法（适配ClientHandler调用）
     */
    public synchronized String makeMove(String xStr, String yStr, String color, Player player) {
        return makeMove(xStr + yStr, color, player);
    }

    /**
     * 玩家准备（start指令触发）
     */
    public synchronized void playerReady(Player player) {
        if (player == player1) player1Ready = true;
        else if (player == player2) player2Ready = true;
        else return;

        ServerLogger.info("玩家[" + player.getName() + "]已准备");

        // 双方都准备则开始游戏
        if (player1Ready && player2Ready) {
            isGameStarted = true;
            isGameOver = false;
            board.reset();
            player1.sendMessage(AnsiColor.color("游戏开始！你是黑棋，黑手先", AnsiColor.GREEN));
            player2.sendMessage(AnsiColor.color("游戏开始！你是白棋，白手后", AnsiColor.GREEN));
            sendMessageToAll(board.toString());
            ServerLogger.success("房间[" + roomId + "]游戏开始");
        } else {
            player.sendMessage(AnsiColor.color("等待对手准备...", AnsiColor.BLUE));
        }
    }

    /**
     * 广播消息给所有玩家（兼容测试模式）
     */
    private void sendMessageToAll(String msg) {
        if (player1 != null) player1.sendMessage(msg);
        if (player2 != null && !isTestMode) player2.sendMessage(msg);
    }

    /**
     * 重置房间状态
     */
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

    // Getter（完整）
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