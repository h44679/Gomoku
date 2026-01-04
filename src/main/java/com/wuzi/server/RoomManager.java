package com.wuzi.server;

import com.wuzi.common.AnsiColor;

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

            int count = room.getPlayerCount();
            String status;

            if (count == 0) {
                status = AnsiColor.color("空房", AnsiColor.GREEN); // 空房绿色
            } else if (count == 1) {
                status = AnsiColor.color("空房", AnsiColor.GREEN); // 空房绿色
            } else {
                status = AnsiColor.color("已满", AnsiColor.RED); // 已满红色
            }

            sb.append("[")
                    .append(roomId)
                    .append("] ")
                    .append(String.format("%-6s", status))
                    .append(" (")
                    .append(count)
                    .append("/2)\n");
        }

        sb.append("\n👉 输入 enter room x 进入房间\n");
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
        boolean added = room.addPlayer(player);
        if (added) {
            ServerLogger.info(player.getName() + " 进入了房间 " + roomId);
            player.setCurrentRoom(room);
        }
        return added;
    }




}