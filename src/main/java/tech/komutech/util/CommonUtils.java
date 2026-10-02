package tech.komutech.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.util.colors.CMIChatColor;

/**
 * 从原 {@code tech.origintech.utils.CommonUtils} 中摘出的两个纯工具方法。
 *
 * <p>原 CommonUtils 的大部分物品解析职责已由 {@link tech.komutech.load.Read} 承接（并去掉了
 * ProjectAddon 依赖），这里只保留被 {@code SingleItemRecipeGuideListener} 等类使用的、
 * 与附属无关的字符串/物品小工具，行为与原实现逐行一致。
 */
public final class CommonUtils {

    private CommonUtils() {}

    /** 在集合中取第一个满足条件的元素（与原实现一致）。 */
    public static <T> T getIf(Iterable<T> iterable, Predicate<T> predicate) {
        if (iterable == null) {
            return null;
        }
        for (T item : iterable) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /** 在物品 lore 末尾追加若干行（{@code blankFirst=true} 时先补一个空行）。 */
    public static void addLore(ItemStack stack, boolean blankFirst, String... lines) {
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore != null) {
            if (blankFirst) {
                lore.add("");
            }
            lore.addAll(CMIChatColor.translate(Arrays.asList(lines)));
        } else {
            lore = CMIChatColor.translate(Arrays.asList(lines));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    /** 去掉数组中的 null 槽位（与原实现一致：机器配方读完后压缩空槽）。 */
    public static ItemStack[] removeNulls(ItemStack[] arr) {
        int n = 0;
        for (ItemStack s : arr) {
            if (s != null) n++;
        }
        ItemStack[] out = new ItemStack[n];
        int i = 0;
        for (ItemStack s : arr) {
            if (s != null) out[i++] = s;
        }
        return out;
    }

    /** 把秒数格式化为 {@code &b12&es} / {@code &b1&emin&b30&es} 形式的 &-码字符串。 */
    public static String formatSeconds(int seconds) {
        if (seconds < 60) {
            return "&b" + seconds + "&es";
        } else if (seconds > 60 && seconds < 3600) {
            int min = seconds / 60;
            int sec = seconds % 60;
            return "&b" + min + "&emin" + (sec != 0 ? "&b" + sec + "&es" : "");
        } else {
            int hour = seconds / 3600;
            int min = seconds % 3600 / 60;
            int sec = seconds % 3600 % 60;
            return "&b" + hour + "&eh"
                + (min != 0 ? "&b" + min + "&emin" : "")
                + (sec != 0 ? "&b" + sec + "&es" : "");
        }
    }
}
