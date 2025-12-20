package com.wuzi.server;

import java.util.HashMap;
import java.util.Map;

public class RoomManager {
    private Map<Integer, GameRoom> roomMap;
    private static final int MAX_ROOM = 10;

    public RoomManager() {
        roomMap = new HashMap<>();
        for (int i = 1; i <= MAX_ROOM; i++) {
            roomMap.put(i, new GameRoom(i));
        }
        ServerLogger.success("房间管理器初始化完成，创建了" + MAX_ROOM + "个房间");
    }

    public String getRoomsStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 房间列表 ===\n");
        for (Map.Entry<Integer, GameRoom> entry : roomMap.entrySet()) {
            int roomId = entry.getKey();
            GameRoom room = entry.getValue();
            sb.append("房间").append(roomId)
                    .append("：人数=").append(room.getPlayerCount())
                    .append("，游戏状态=").append(room.isGameStarted() ? "已开始" : "未开始")
                    .append("\n");
        }
        return sb.toString();
    }

    public GameRoom getRoom(int roomId) {
        return roomMap.get(roomId);
    }

    public void removePlayerFromRoom(Player player) {
        if (player == null) return;
        GameRoom room = player.getCurrentRoom();
        if (room != null) room.removePlayer(player);
    }

    public boolean addPlayerToRoom(int roomId, Player player) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            ServerLogger.error("房间" + roomId + "不存在");
            return false;
        }
        removePlayerFromRoom(player);
        return room.addPlayer(player);
    }
}