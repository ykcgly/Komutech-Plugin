package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.script.ScriptEval;
import tech.komutech.util.CommonUtils;
import tech.komutech.util.ExceptionHandler;

/**
 * 加载 menus.yml（自定义机器菜单）。
 *
 * <p>严格复刻原 {@code MenuReader.readEach} 的分支与默认值：
 * <ul>
 *   <li>{@code title} 缺省空串；{@code playerInvClickable} 缺省 true；{@code size} 缺省 -1
 *       （显式给出且非 9 的倍数 → 报错跳过）；</li>
 *   <li>{@code import}：先按 id 查 Slimefun 已注册的 {@code BlockMenuPreset}，查不到再查本插件
 *       菜单表克隆（本服内容包 0 处使用，保留以兼容后续新增）；</li>
 *   <li>{@code matrix}：按字符矩阵展开槽位并支持 {@code mapping} 映射（本服 0 处使用）；</li>
 *   <li>{@code slots}：键可为单个槽位数字或 {@code a-b} 区间；越界槽位跳过；</li>
 *   <li>{@code progressbar: true} 的槽位成为进度条槽，物品可用 {@code progressBarItem} 覆盖。</li>
 * </ul>
 *
 * <p><b>刻意保留的一处原实现行为</b>：进度条物品在设置完 PDC 后<b>没有</b>写回
 * {@code setItemMeta}——Bukkit 的 {@code getItemMeta()} 返回副本，因此这段赋值实际不生效。
 * 按「不改变物品行为」原则原样复刻，不做修复。
 *
 * <p>菜单必须在机器之前加载（机器按 {@code upper(id)} 从 {@link KT#menus} 取菜单），
 * 顺序由 {@link Setup#loadAll} 保证。
 */
public final class MenusLoader {

    private MenusLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "menus.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                CustomMenu menu = read(id, s);
                if (menu == null) {
                    skip++;
                } else {
                    KT.menus.put(KT.upper(id), menu);
                    ok++;
                }
            } catch (Exception e) {
                KT.log("menus.yml " + id + " 加载失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("menus.yml: 加载 " + ok + ", 跳过 " + skip);
    }

    static CustomMenu read(String id, ConfigurationSection s) {
        if (ExceptionHandler.handleMenuConflict(id) == ExceptionHandler.HandleResult.FAILED) {
            return null;
        }

        String title = s.getString("title", "");
        boolean playerInvClickable = s.getBoolean("playerInvClickable", true);
        int size = s.getInt("size", -1);
        if (s.contains("size") && size != -1 && size % 9 != 0) {
            KT.log("加载菜单 " + id + " 时遇到了问题: 菜单大小必须是9的倍数。");
            return null;
        }

        ScriptEval eval = ScriptLoader.load(s.getString("script"), "菜单", id);

        if (s.contains("import")) {
            String imported = s.getString("import", "");
            BlockMenuPreset preset = Slimefun.getRegistry().getMenuPresets().get(imported);
            if (preset == null) {
                CustomMenu src = CommonUtils.getIf(KT.menus.values(), x -> x.getId().equalsIgnoreCase(imported));
                if (src == null) {
                    KT.log("加载菜单 " + id + " 时遇到了问题: 无法找到要导入的菜单 " + imported);
                    return null;
                }
                return new CustomMenu(id, title, src);
            }
            return new CustomMenu(id, title, preset, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), eval);
        }

        int progressSlot = 22;
        ItemStack progress = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

        if (s.contains("matrix")) {
            List<String> matrix = s.getStringList("matrix");
            Map<Integer, ItemStack> items = new HashMap<>();
            Map<Character, ItemStack> mapping = new HashMap<>();
            int idx = 0;
            for (String row : matrix) {
                for (char c : row.toCharArray()) {
                    if (!mapping.containsKey(c)) {
                        ConfigurationSection sec = s.getConfigurationSection("mapping." + c);
                        if (sec == null) {
                            idx++;
                            continue;
                        }
                        mapping.put(c, Read.item(sec, true));
                        if (sec.getBoolean("progressbar", false)) {
                            progressSlot = idx;
                            progress = sec.contains("progressBarItem")
                                ? Read.item(sec.getConfigurationSection("progressBarItem"), true)
                                : mapping.get(c);
                            markProgress(progress);
                        }
                    }
                    items.put(idx, mapping.get(c));
                    idx++;
                }
            }
            return new CustomMenu(id, title, items, playerInvClickable, progressSlot, progress, eval);
        }

        Map<Integer, ItemStack> items = new HashMap<>();
        ConfigurationSection slots = s.getConfigurationSection("slots");
        if (slots == null) {
            KT.log("加载菜单 " + id + " 时遇到了问题: 没有设置物品。");
            return null;
        }

        for (String rawSlot : slots.getKeys(false)) {
            try {
                int slot = Integer.parseInt(rawSlot);
                if (slot > 53 || slot < 0) {
                    KT.log("菜单 " + id + " 槽位 " + slot + " 越界，跳过");
                    continue;
                }
                ConfigurationSection sec = slots.getConfigurationSection(rawSlot);
                ItemStack stack = Read.item(sec, true);
                if (stack == null) {
                    KT.log("菜单 " + id + " 槽位 " + slot + " 的物品格式错误，跳过");
                } else {
                    if (sec.getBoolean("progressbar", false)) {
                        progressSlot = slot;
                        progress = sec.contains("progressBarItem")
                            ? Read.item(sec.getConfigurationSection("progressBarItem"), true)
                            : stack;
                        markProgress(progress);
                    }
                    items.put(slot, stack);
                }
            } catch (NumberFormatException e) {
                String[] range = rawSlot.split("-");
                if (range.length != 2) {
                    KT.log("菜单 " + id + " 有错误的槽位区间表达式 " + rawSlot);
                    continue;
                }
                ConfigurationSection sec = slots.getConfigurationSection(rawSlot);
                ItemStack stack = Read.item(sec, true);
                if (stack == null) {
                    KT.log("菜单 " + id + " 区间槽位 " + rawSlot + " 的物品格式错误，跳过");
                    continue;
                }
                try {
                    IntStream.rangeClosed(Integer.parseInt(range[0]), Integer.parseInt(range[1]))
                        .forEach(slot -> {
                            if (slot <= 53 && slot >= 0) {
                                items.put(slot, stack);
                            } else {
                                KT.log("菜单 " + id + " 区间槽位 " + slot + " 越界，跳过");
                            }
                        });
                } catch (NumberFormatException ex) {
                    KT.log("菜单 " + id + " 有错误的槽位区间表达式 " + rawSlot);
                }
            }
        }

        return new CustomMenu(id, title, items, playerInvClickable, progressSlot, progress, eval).setSize(size);
    }

    /**
     * 给进度条物品打 {@code progress} PDC。
     * <p>原实现在此处取 {@code getItemMeta()} 后设置 PDC 却<b>未</b>写回，故实际不生效；
     * 这里保持同一写法（含未写回），以免改变运行期行为。
     */
    private static void markProgress(ItemStack progress) {
        if (progress == null) return;
        NamespacedKey key = new NamespacedKey(KT.plugin(), "progress");
        ItemMeta meta = progress.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(key, PersistentDataType.INTEGER, 0);
        }
    }
}
