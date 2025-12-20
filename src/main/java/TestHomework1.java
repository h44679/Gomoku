import com.wuzi.common.AnsiColor;
import com.wuzi.server.ServerLogger;

public class TestHomework1 {
    public static void main(String[] args) {
        System.out.println("--- 1. 测试颜色工具 ---");
        // 红色文字
        System.out.println(AnsiColor.color("Error Text", AnsiColor.RED));
        // 红色背景（高亮棋子）
        System.out.println(AnsiColor.color(" Last Move ", AnsiColor.BG_RED));

        System.out.println("\n--- 2. 测试日志格式 ---");
        ServerLogger.info("服务器正在初始化...");
        ServerLogger.warn("CPU 温度过高 (模拟警告)");
        ServerLogger.error("数据库连接失败！");
        ServerLogger.success("系统启动完成。");
    }
}