import com.wuzi.server.GameRoom;
import com.wuzi.server.Player;

/**
 * 本地测试类（左右互搏，适配0-E坐标，彻底修复int转String错误）
 */
public class TestHomework3 {
    public static void main(String[] args) {
        // 1. 创建测试房间（开启测试模式，无网络）
        GameRoom room = new GameRoom(888, true);
        room.setTestMode(true);

        // 2. 创建玩家（Socket/PrintWriter传null，纯本地测试）
        Player alice = new Player("Alice", null, null);
        Player bob = new Player("Bob", null, null);

        // 3. 添加玩家到房间
        room.addPlayer(alice);
        room.addPlayer(bob);
        // 4. 启动游戏
        room.startGame();

        // 5. 打印当前执子（严格匹配测试要求）
        System.out.println("当前执子: " + room.getCurrentTurnColor());

        // ========== 核心修复：所有坐标必须传String类型（加引号） ==========
        // 6. 黑棋落子（7,7）→ 传"7"而非7
        boolean p1Move = room.makeMove("7", "7", "black", alice).contains("成功");
        System.out.println("黑棋落子: " + p1Move); // true

        // 7. 黑棋企图连下（7,8）→ 传"7"、"8"（String类型）
        boolean p1Again = room.makeMove("7", "8", "black", alice).contains("成功");
        System.out.println("黑棋连下: " + p1Again); // false

        // 8. 白棋落子（7,8）→ 传"7"、"8"（String类型）
        boolean p2Move = room.makeMove("7", "8", "white", bob).contains("成功");
        System.out.println("白棋落子: " + p2Move); // true

        // 可选：测试0-E字母坐标（如A=10）
        boolean p1AE = room.makeMove("A", "7", "black", alice).contains("成功");
        System.out.println("黑棋落子A,7: " + p1AE); // true
    }
}