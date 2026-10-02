package tech.komutech;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.generations.GenerationInfo;

/**
 * 全局静态持有（对齐 WorldTaste 的 WT）：插件实例、物品组表、自定义配方类型表、
 * 以及加载期「展示物品堆」表。
 *
 * <p>{@link #preload} 是本架构的关键：加载前先把各内容文件里每个 id 的展示堆解析出来按
 * 大写 id 索引，后续任意文件解析配方 / 引用其它物品时都能立刻查到，从而让
 * items.yml 可以引用 mob_drops.yml、机器产物可以引用 items.yml，不受文件加载顺序限制。
 * 加载结束后由 Setup 清空释放（运行期不再需要）。
 */
public final class KT {

    private KT() {}

    public static KomutechPlugin plugin;

    /** 展示物品堆：大写 id -> ItemStack（加载期填充，加载结束清空）。 */
    public static final Map<String, ItemStack> preload = new HashMap<>();

    /** 物品组：小写 key -> ItemGroup。 */
    public static final Map<String, ItemGroup> groups = new HashMap<>();

    /** 自定义配方类型：大写 id -> RecipeType。 */
    public static final Map<String, RecipeType> recipeTypes = new HashMap<>();

    /** 自定义菜单：大写 id -> CustomMenu（机器/工作台按 id 取用）。 */
    public static final Map<String, CustomMenu> menus = new HashMap<>();

    /** 世界生成条目（原实现按附属分别持有，单插件形态下合并为一份全局表）。 */
    public static final List<GenerationInfo> generations = new ArrayList<>();

    public static void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    /** 按 item_group 取组（大小写不敏感）。 */
    public static ItemGroup group(String id) {
        if (id == null) return null;
        return groups.get(id.toLowerCase(Locale.ROOT));
    }

    /** 统一的大写 id 规范化：物品注册时 id 一律大写（与原插件注册行为一致）。 */
    public static String upper(String id) {
        return id == null ? null : id.toUpperCase(Locale.ROOT);
    }

    /** 当前插件实例（供脚本层 / 工具类使用，避免到处传 plugin）。 */
    public static KomutechPlugin plugin() {
        return plugin;
    }
}
