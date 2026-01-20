package com.wuzi.server;

import com.wuzi.common.AnsiColor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Collection;

public class RoomManager {
    // 使用 ConcurrentHashMap 保证线程安全
    private final Map<Integer, GameRoom> roomMap;
    // 使用 AtomicInteger 保证多线程下 ID 不重复
    private final AtomicInteger idGenerator;

    public RoomManager() {
        //核心原因是ConcurrentHashMap的特性更适配“房间ID与房间实例绑定管理”的需求
        //销毁房间时，HashMap可通过ID直接删除对应元素，效率高；ArrayList删除元素时会导致后续元素移位，效率较低，且删除后若依赖索引对应ID，会出现关联错乱。
        this.roomMap = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicInteger(1);

        // 默认创建 10 个初始房间
        for (int i = 0; i < 10; i++) {
            createRoom();
        }
        ServerLogger.success("房间管理器初始化完成，默认创建了 10 个初始房间");
    }

    /**
     * 创建房间生成唯一id  该代码的核心作用是在多线程环境下生成全局唯一的房间ID，为每个房间分配专属标识
     */
    public GameRoom createRoom() {
        //getAndIncrement()方法能保证原子性自增，即多线程同时创建房间时，不会出现ID重复的情况，确保每个房间有唯一标识，避免房间管理混乱。
        int id = idGenerator.getAndIncrement();
        GameRoom room = new GameRoom(id);
        //此处roomMap.put(id, room)是HashMap的核心方法，作用是将生成的房间ID与房间实例绑定，存入线程安全的映射集合中，完成房间的注册管理
        roomMap.put(id, room);
        ServerLogger.info("房间 " + id + " 已创建");
        return room;
    }

    /**
     * 核心修复：处理玩家进入房间的逻辑
     * 解决了 Copilot 代码调用 addPlayerToRoom 报错的问题
     */
    public boolean addPlayerToRoom(int roomId, Player player) {
        if (player == null) return false;

        // 1. 自动处理：如果玩家已在其他房间，先安全退出
        GameRoom currentRoom = player.getCurrentRoom();
        if (currentRoom != null) {
            // 如果已经在目标房间了，直接返回成功
            if (currentRoom.getRoomId() == roomId) return true;
            currentRoom.removePlayer(player);
        }

        // 2. 检查目标房间是否存在 (直接使用 roomMap，因为都在同一个类里),当房间不存在（roomMap中无对应ID的键值对）时，roomMap.get(roomId)会返回null
        GameRoom targetRoom = roomMap.get(roomId);
        if (targetRoom == null) {
            ServerLogger.warn("玩家[" + player.getName() + "]尝试进入不存在的房间: " + roomId);
            return false;
        }

        // 3. 尝试加入新房间
        return targetRoom.addPlayer(player);
    }

    /**
     * 销毁房间
     */
    public void removeRoom(int roomId) {
        if (roomMap.containsKey(roomId)) {
            roomMap.remove(roomId);
            ServerLogger.warn("房间 " + roomId + " 已被销毁回收");
        }
    }

    public GameRoom getRoom(int roomId) {
        return roomMap.get(roomId);
    }

    public Collection<GameRoom> getAllRooms() {
        return roomMap.values();
    }

    /**
     * 获取所有房间状态
     */
    public String getRoomsStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 房间列表 ===\n");

        for (GameRoom room : roomMap.values()) {
            int roomId = room.getRoomId();
            int count = room.getPlayerCount();
            String status = (count < 2) ?
                    AnsiColor.color("有空位", AnsiColor.GREEN) :
                    AnsiColor.color("已满", AnsiColor.RED);

            sb.append("[").append(roomId).append("] ")
                    .append(status).append(" (")
                    .append(count).append("/2)\n");
        }
        sb.append("\n👉 输入 enter room x 进入房间\n");
        return sb.toString();
    }

    /**
     * 将玩家从当前房间移除
     */
    public void removePlayerFromRoom(Player player) {
        if (player == null) return;
        GameRoom room = player.getCurrentRoom();
        if (room != null) {
            room.removePlayer(player);
        }
    }
    // RoomManager 类中新增
    public synchronized boolean destroyRoom(int roomId) {
        if (!roomMap.containsKey(roomId)) {
            ServerLogger.error("物理销毁房间失败：房间[" + roomId + "]不存在");
            return false;
        }
        ServerLogger.info("房间[" + roomId + "]已物理销毁：从房间管理器中移除，剩余房间数：" + (roomMap.size() - 1));
        roomMap.remove(roomId);
        return true;
    }
}