package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.behavior.BlockDrops;
import tech.komutech.objects.customs.item.CustomGeoResource;

/**
 * 加载 geo_resources.yml（GEO 自然资源，可被地质采矿机获取）。
 *
 * <p>复刻原 {@code GeoResourceReader.readEach}：{@code max_deviation} 缺省 1、
 * {@code obtain_from_geo_miner} 缺省 true、{@code supply} 支持「环境→数量」与
 * 「环境→(群系→数量, others 兜底)」两级结构，{@code Environment.CUSTOM} 恒返回 0。
 * 若带 {@code drop_from} 则同时登记原版方块掉落（交由 {@link BlockDrops} 统一处理）。
 */
public final class GeoResourcesLoader {

    private GeoResourcesLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "geo_resources.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log("geo_resources.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("geo_resources.yml: 注册 " + ok + ", 跳过 " + skip);
    }

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

        int maxDeviation = s.getInt("max_deviation", 1);
        boolean obtainable = s.getBoolean("obtain_from_geo_miner", true);
        String geoName = s.getString("geo_name", "");
        RecipeType rt = RecipeTypes.resolve(s.getString("recipe_type", "NULL"));
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);

        ConfigurationSection supply = s.getConfigurationSection("supply");
        BiFunction<Environment, Biome, Integer> supplyFn = (env, biome) -> {
            if (supply == null) return 0;
            if (env == Environment.CUSTOM) return 0;
            String envKey = env.toString().toLowerCase(Locale.ROOT);
            String biomeKey = biome.toString().toLowerCase(Locale.ROOT);
            if (!supply.isConfigurationSection(envKey)) {
                return supply.getInt(envKey, 0);
            }
            ConfigurationSection sec = supply.getConfigurationSection(envKey);
            if (sec == null) return 0;
            return sec.contains(biomeKey) ? sec.getInt(biomeKey, 0) : sec.getInt("others", 0);
        };

        if (s.contains("drop_from")) {
            int chance = s.getInt("drop_chance", 100);
            if (chance < 0 || chance > 100) {
                KT.log("自然资源 " + id + ": 掉落几率 " + chance + " 不在 0-100，已转为 100");
                chance = 100;
            }
            String rawMaterial = s.getString("drop_from", "");
            Optional<Material> mat = Optional.ofNullable(Material.matchMaterial(rawMaterial));
            if (mat.isEmpty()) {
                KT.log("自然资源 " + id + ": 掉落方块材料类型 " + rawMaterial + " 不存在");
            } else if (s.isInt("drop_amount")) {
                int amount = s.getInt("drop_amount", 1);
                BlockDrops.add(mat.get(), effId, chance, amount, amount);
            } else {
                // drop_amount 写成 "1-3" 区间形式
                int[] range = BlockDrops.parseAmountRange(s.getString("drop_amount", "1"));
                BlockDrops.add(mat.get(), effId, chance, range[0], range[1]);
            }
        }

        // 构造器内部已自行 register()（GEOResource 注册 + SlimefunItem 注册），勿重复调用
        new CustomGeoResource(g, sfis, rt, recipe, supplyFn, maxDeviation, obtainable, geoName);
        return true;
    }
}
