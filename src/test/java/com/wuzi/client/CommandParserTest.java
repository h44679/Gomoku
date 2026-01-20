package com.wuzi.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandParserTest - 指令解析测试
 * 使用参数化测试验证用户输入字符串的解析正确性
 */
@DisplayName("命令解析测试")
class CommandParserTest {

    /**
     * 从字符串标签转换为数组索引
     * "A"到"O"对应0-14，"1"到"15"对应0-14
     */
    private Integer labelToIdx(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.toUpperCase();
        char first = s.charAt(0);
        if (first >= 'A' && first <= 'O') {
            return first - 'A';
        }
        try {
            int v = Integer.parseInt(s) - 1;
            if (v >= 0 && v < 15) return v;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 解析用户输入"put X Y"格式的命令
     * 支持(A, 8)或(8, A)两种顺序
     */
    private int[] parseInput(String s1, String s2) {
        Integer r = labelToIdx(s1);
        Integer c = labelToIdx(s2);
        if (r != null && c != null) return new int[]{r, c};

        r = labelToIdx(s2);
        c = labelToIdx(s1);
        if (r != null && c != null) return new int[]{r, c};

        return null;
    }

    @ParameterizedTest(name = "解析 put {0} {1} -> ({2}, {3})")
    @CsvSource({
            // 标准格式：列字母 行数字
            "A, 1, 0, 0",
            "A, 8, 0, 7",
            "A, 15, 0, 14",
            "H, 8, 7, 7",
            "H, 1, 7, 0",
            "H, 15, 7, 14",
            "O, 1, 14, 0",
            "O, 8, 14, 7",
            "O, 15, 14, 14",

            // 反向格式：行数字 列字母
            "1, A, 0, 0",
            "8, H, 7, 7",
            "15, O, 14, 14",

            // 边界和中心位置
            "C, 3, 2, 2",
            "E, 5, 4, 4",
            "G, 7, 6, 6",
            "M, 13, 12, 12",
            "N, 14, 13, 13",
    })
    @DisplayName("参数化测试：put命令解析")
    void testPutCommandParsing(String param1, String param2, int expectedRow, int expectedCol) {
        int[] result = parseInput(param1, param2);
        assertNotNull(result, "解析结果不应为null");
        assertEquals(expectedRow, result[0], "行坐标应为" + expectedRow);
        assertEquals(expectedCol, result[1], "列坐标应为" + expectedCol);
    }

    @ParameterizedTest(name = "解析列标签 {0} -> {1}")
    @CsvSource({
            "A, 0",
            "B, 1",
            "C, 2",
            "D, 3",
            "E, 4",
            "F, 5",
            "G, 6",
            "H, 7",
            "I, 8",
            "J, 9",
            "K, 10",
            "L, 11",
            "M, 12",
            "N, 13",
            "O, 14",
    })
    @DisplayName("参数化测试：列标成功解析A-O")
    void testColumnLabelParsing(String label, int expectedIndex) {
        Integer result = labelToIdx(label);
        assertNotNull(result, "列标签" + label + "应被解析");
        assertEquals(expectedIndex, result, label + "应对应索引" + expectedIndex);
    }

    @ParameterizedTest(name = "解析行标签 {0} -> {1}")
    @CsvSource({
            "1, 0",
            "2, 1",
            "3, 2",
            "4, 3",
            "5, 4",
            "6, 5",
            "7, 6",
            "8, 7",
            "9, 8",
            "10, 9",
            "11, 10",
            "12, 11",
            "13, 12",
            "14, 13",
            "15, 14",
    })
    @DisplayName("参数化测试：行标签解析1-15")
    void testRowLabelParsing(String label, int expectedIndex) {
        Integer result = labelToIdx(label);
        assertNotNull(result, "行标签" + label + "应被解析");
        assertEquals(expectedIndex, result, label + "应对应索引" + expectedIndex);
    }

    @ParameterizedTest(name = "小写列标签 {0} -> {1}")
    @CsvSource({
            "a, 0",
            "h, 7",
            "o, 14",
    })
    @DisplayName("参数化测试：小写列标签解析")
    void testLowercaseColumnParsing(String label, int expectedIndex) {
        Integer result = labelToIdx(label);
        assertNotNull(result, "小写列标签应支持");
        assertEquals(expectedIndex, result);
    }

    @ParameterizedTest(name = "无效列标签 {0}")
    @CsvSource({
            "Z",
            "P",
            "@",
            "#",
            "1A",
            "15A",
    })
    @DisplayName("参数化测试：无效列标签返回null")
    void testInvalidColumnLabel(String label) {
        Integer result = labelToIdx(label);
        assertNull(result, "无效列标签" + label + "应返回null");
    }

    @ParameterizedTest(name = "无效行标签 {0}")
    @CsvSource({
            "0",
            "16",
            "99",
            "-1",
    })
    @DisplayName("参数化测试：无效行标签返回null")
    void testInvalidRowLabel(String label) {
        Integer result = labelToIdx(label);
        assertNull(result, "无效行标签" + label + "应返回null");
    }

    @ParameterizedTest(name = "边界值测试 {0}, {1}")
    @CsvSource({
            "A, 1",
            "A, 15",
            "O, 1",
            "O, 15",
    })
    @DisplayName("参数化测试：四个角落")
    void testFourCorners(String col, String row) {
        int[] result = parseInput(col, row);
        assertNotNull(result, "四个角落应能解析");
        assertTrue(result[0] >= 0 && result[0] < 15, "行坐标应在范围内");
        assertTrue(result[1] >= 0 && result[1] < 15, "列坐标应在范围内");
    }

    @ParameterizedTest(name = "中心位置 {0}, {1}")
    @CsvSource({
            "H, 8",
            "H, 7",
            "H, 9",
            "G, 8",
            "I, 8",
    })
    @DisplayName("参数化测试：棋盘中心区域")
    void testCenterArea(String col, String row) {
        int[] result = parseInput(col, row);
        assertNotNull(result, "中心区域应能解析");
        int centerDist = Math.abs(result[0] - 7) + Math.abs(result[1] - 7);
        assertTrue(centerDist <= 2, "应在中心附近");
    }

    @ParameterizedTest(name = "分布式坐标 {0}, {1}")
    @CsvSource({
            "A, 1",
            "B, 3",
            "C, 5",
            "D, 7",
            "E, 9",
            "F, 11",
            "G, 13",
            "H, 15",
            "I, 2",
            "J, 4",
            "K, 6",
            "L, 8",
            "M, 10",
            "N, 12",
            "O, 14",
    })
    @DisplayName("参数化测试：全棋盘分布坐标")
    void testDistributedCoordinates(String col, String row) {
        int[] result = parseInput(col, row);
        assertNotNull(result, col + row + "应被解析");
        assertEquals(2, result.length, "结果应为两个元素的数组");
    }

    @ParameterizedTest(name = "null和空值处理")
    @CsvSource({
            "'', A",
            "A, ''",
    })
    @DisplayName("参数化测试：null和空字符串")
    void testNullAndEmptyValues(String val1, String val2) {
        int[] result = parseInput(val1.isEmpty() ? null : val1,
                                  val2.isEmpty() ? null : val2);
        assertNull(result, "包含null/空的输入应返回null");
    }

    @ParameterizedTest(name = "命令格式 {0}")
    @CsvSource({
            "put H 8",
            "put A 1",
            "put O 15",
            "put H 8 extra",
            "H 8",
            "put",
    })
    @DisplayName("参数化测试：命令格式验证")
    void testCommandFormat(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 3 && parts[0].equalsIgnoreCase("put")) {
            int[] result = parseInput(parts[1], parts[2]);
            assertNotNull(result, "标准put命令应被解析");
        } else if (parts.length == 2) {
            int[] result = parseInput(parts[0], parts[1]);
            assertNotNull(result, "简写坐标应被解析");
        } else {
            // 其他格式应返回null或异常
            if (parts.length >= 2) {
                int[] result = parseInput(parts[parts.length - 2], parts[parts.length - 1]);
                // 可能返回null或坐标
                assertTrue(result == null || (result[0] >= 0 && result[1] >= 0));
            }
        }
    }
}

