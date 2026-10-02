package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

/**
 * 加载 recipe_types.yml 自定义配方类型（键为大写 id，NamespacedKey 用小写——
 * 与原 RecipeTypesReader 一致），并提供 recipe_type 字符串解析。
 */
public final class RecipeTypes {

    private RecipeTypes() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "recipe_types.yml");
        int ok = 0;
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) continue;
            try {
                // Komutech 的 recipe_types 段把 name/lore/material_type/material 直接放在段下，
                // 与 Read.item 读取的键一致，可直接复用
                ItemStack display = Read.item(s, false);
                if (display == null) {
                    KT.log("recipe_types " + key + ": 无图标，跳过");
                    continue;
                }
                RecipeType rt = new RecipeType(new NamespacedKey(KT.plugin, key.toLowerCase(Locale.ROOT)), display);
                KT.recipeTypes.put(key.toUpperCase(Locale.ROOT), rt);
                ok++;
            } catch (Exception e) {
                KT.log("recipe_types " + key + " 失败: " + e);
            }
        }
        KT.plugin.getLogger().info("recipe_types.yml: 注册 " + ok);
    }

    /** 解析 recipe_type 字符串：先查自定义，再映射标准常量，最后回退 NULL。 */
    public static RecipeType resolve(String name) {
        if (name == null) return RecipeType.NULL;
        String u = name.toUpperCase(Locale.ROOT);
        if (u.equals("NULL") || u.isEmpty()) return RecipeType.NULL;
        RecipeType custom = KT.recipeTypes.get(u);
        if (custom != null) return custom;
        switch (u) {
            case "ENHANCED_CRAFTING_TABLE": return RecipeType.ENHANCED_CRAFTING_TABLE;
            case "MOB_DROP": return RecipeType.MOB_DROP;
            case "BARTER_DROP": return RecipeType.BARTER_DROP;
            case "MULTIBLOCK": return RecipeType.MULTIBLOCK;
            case "SMELTERY": return RecipeType.SMELTERY;
            case "GRIND_STONE": return RecipeType.GRIND_STONE;
            case "ORE_CRUSHER": return RecipeType.ORE_CRUSHER;
            case "COMPRESSOR": return RecipeType.COMPRESSOR;
            case "PRESSURE_CHAMBER": return RecipeType.PRESSURE_CHAMBER;
            case "MAGIC_WORKBENCH": return RecipeType.MAGIC_WORKBENCH;
            case "ORE_WASHER": return RecipeType.ORE_WASHER;
            case "JUICER": return RecipeType.JUICER;
            case "ANCIENT_ALTAR": return RecipeType.ANCIENT_ALTAR;
            case "INTERACT": return RecipeType.INTERACT;
            case "FOOD_FABRICATOR": return RecipeType.FOOD_FABRICATOR;
            case "FOOD_COMPOSTER": return RecipeType.FOOD_COMPOSTER;
            case "FREEZER": return RecipeType.FREEZER;
            case "REFINERY": return RecipeType.REFINERY;
            case "GEO_MINER": return RecipeType.GEO_MINER;
            case "ARMOR_FORGE": return RecipeType.ARMOR_FORGE;
            case "GOLD_PAN": return RecipeType.GOLD_PAN;
            case "HEATED_PRESSURE_CHAMBER": return RecipeType.HEATED_PRESSURE_CHAMBER;
            default:
                MissingItems.record("未知recipe_type:" + name);
                return RecipeType.NULL;
        }
    }
}
