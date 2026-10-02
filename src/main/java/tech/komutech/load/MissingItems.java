package tech.komutech.load;

import java.util.LinkedHashMap;
import java.util.Map;
import tech.komutech.KT;

/**
 * 「物品未找到」折叠上报：未装对应前置附属时此类引用可达数千处，逐条刷屏会淹没日志，
 * 故加载期只计数，结束后汇总为一行。
 */
public final class MissingItems {

    private MissingItems() {}

    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();

    public static void reset() {
        COUNTS.clear();
    }

    public static void record(String key) {
        COUNTS.merge(key, 1, Integer::sum);
    }

    public static void report() {
        if (COUNTS.isEmpty()) return;
        int total = COUNTS.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder sb = new StringBuilder("未找到的引用共 ").append(total).append(" 处：");
        int shown = 0;
        for (Map.Entry<String, Integer> e : COUNTS.entrySet()) {
            if (shown++ >= 10) {
                sb.append(" …等 ").append(COUNTS.size()).append(" 种");
                break;
            }
            sb.append("\n  ").append(e.getKey()).append(" ×").append(e.getValue());
        }
        KT.log(sb.toString());
    }
}
