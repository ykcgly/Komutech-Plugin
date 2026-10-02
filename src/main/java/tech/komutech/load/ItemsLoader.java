package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.behavior.BlockDrops;

/**
 * 加载普通物品（items.yml）。
 *
 * <p>machines.yml 不在这里处理 —— 它由 {@link MachinesLoader} 注册为真正的机器
 * （带 input/output 槽位、能源、脚本行为），若当普通物品注册会丢失全部机器语义。
 *
 * <p>两遍注册：先非 lateInit，再 lateInit —— 与原 YamlReader 的 readAll + loadLateInits 一致，
 * 保证「依赖其它物品先存在」的项排在后面。
 *
 * <p>id 处理：有效 id 取 {@code id_alias}（无则取键名）并统一大写，与原 ProjectAddon.getId 一致。
 */
public final class ItemsLoader {

    private ItemsLoader() {}

    public static void load() {
        loadFile("items.yml");
    }

    public static void loadFile(String file) {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, file);
        int ok = 0, skip = 0;
        List<ConfigurationSection> late = new ArrayList<>();
        List<String> lateIds = new ArrayList<>();
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            if (s.getBoolean("lateInit", false)) {
                late.add(s);
                lateIds.add(id);
                continue;
            }
            try {
                if (register(id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log(file + " " + id + " 注册失败: " + e);
                skip++;
            }
        }
        for (int i = 0; i < late.size(); i++) {
            try {
                if (register(lateIds.get(i), late.get(i))) ok++; else skip++;
            } catch (Exception e) {
                KT.log(file + " " + lateIds.get(i) + "(lateInit) 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info(file + ": 注册 " + ok + ", 跳过 " + skip);
    }

    /** 通用物品注册。成功返回 true。 */
    static boolean register(String id, ConfigurationSection s) {
        if (!RegisterConditions.pass(s)) return false;

        String effId = KT.upper(s.getString("id_alias", id));
        ItemGroup g = KT.group(s.getString("item_group"));
        if (g == null) {
            MissingItems.record("物品组缺失:" + s.getString("item_group"));
            return false;
        }
        ItemStack display = KT.preload.get(effId);
        if (display == null) display = KT.preload.get(KT.upper(id));
        if (display == null) {
            MissingItems.record("无展示物品:" + effId);
            return false;
        }

        SlimefunItemStack sfis = new SlimefunItemStack(effId, display);
        // 配方格数：infinity_forge 为 36 格，其余 9 格（与原 CommonUtils.getRecipe 一致）
        String rtName = s.getString("recipe_type", "NULL");
        int size = "infinity_forge".equalsIgnoreCase(rtName) ? 36 : 9;
        RecipeType rt = RecipeTypes.resolve(rtName);
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), size);

        SlimefunItem item = new SlimefunItem(g, sfis, rt, recipe);
        if (s.getBoolean("hidden", false)) item.setHidden(true);
        if (s.getBoolean("vanilla", false)) item.setUseableInWorkbench(true);
        item.register(KT.plugin);

        // 方块破坏掉落（drop_from / drop_chance / drop_amount）
        String dropFrom = s.getString("drop_from");
        if (dropFrom != null && !dropFrom.isEmpty()) {
            Material block = Material.matchMaterial(dropFrom);
            if (block != null) {
                int chance = s.getInt("drop_chance", 100);
                if (chance < 0 || chance > 100) chance = 100;
                int[] range = BlockDrops.parseAmountRange(s.getString("drop_amount", "1"));
                BlockDrops.add(block, effId, chance, range[0], range[1]);
            } else {
                KT.log("未知的 drop_from 方块: " + dropFrom);
            }
        }
        return true;
    }
}
