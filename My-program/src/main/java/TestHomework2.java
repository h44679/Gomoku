import com.wuzi.server.GameBoard;

public class TestHomework2 {
    public static void main(String[] args) {
        GameBoard board = new GameBoard();

        // 1. 测试越界落子
        boolean outOfBound = board.makeMove(-1, 0, 1);
        System.out.println("越界落子是否成功：" + outOfBound); // 应该false

        // 2. 测试重复落子
        board.makeMove(7, 7, 1);
        boolean repeat = board.makeMove(7, 7, 2);
        System.out.println("重复落子是否成功：" + repeat); // 应该false

        // 3. 测试获胜判定（水平5子）
        System.out.println("\n--- 测试水平5子获胜 ---");
        board.reset();
        for (int i = 0; i < 5; i++) {
            board.makeMove(14, i, 1);
            System.out.println("落子(14," + i + ")后棋盘：");
            System.out.println(board);
            if (board.checkWin(14, i)) {
                System.out.println("第" + (i+1) + "子落下后获胜！");
                break;
            }
        }

        // 4. 测试斜向获胜
        System.out.println("\n--- 测试斜向5子获胜 ---");
        board.reset();
        for (int i = 0; i < 5; i++) {
            board.makeMove(i, i, 2);
            if (board.checkWin(i, i)) {
                System.out.println("落子(" + i + "," + i + ")后获胜！");
                System.out.println(board);
                break;
            }
        }
    }
}