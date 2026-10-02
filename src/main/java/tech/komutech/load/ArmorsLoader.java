package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.komutech.KT;
import tech.komutech.native_scripts.support.PlayerEffects;
import tech.komutech.objects.customs.item.CustomArmorPiece;
import tech.komutech.util.ExceptionHandler;

/**
 * 加载 armors.yml（盔甲套）。
 *
 * <p>复刻原 {@code ArmorReader}：一套盔甲下按 helmet/chestplate/leggings/boots 四段分别注册
 * 一个 {@link CustomArmorPiece}，部件 id 为 {@code <套id>_<部件大写>}（可被引用的
 * {@code id_alias} 覆盖）；{@code protection_types} 按 {@link ProtectionType} 枚举解析，
 * {@code potion_effects} 每行 {@code "类型 等级"}，持续时长取
 * {@code max(options.armor-update-interval, 20) + 180} 秒换算的 tick。
 */
public final class ArmorsLoader {

    private ArmorsLoader() {}

    private static final List<String> CHECKS = List.of("helmet", "chestplate", "leggings", "boots");

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "armors.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                int n = registerSet(id, s);
                if (n > 0) ok += n; else skip++;
            } catch (Exception e) {
                KT.log("armors.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("armors.yml: 注册部件 " + ok + ", 跳过套装 " + skip);
    }

    /** 预加载：把四件的展示堆按各自 id 放进 KT.preload，供配方互相引用。 */
    public static void preload(YamlConfiguration y) {
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            for (String part : CHECKS) {
                ConfigurationSection p = s.getConfigurationSection(part);
                if (p == null) continue;
                String partId = partId(id, s, p);
                ItemStack display = Read.item(p, false);
                if (display != null) {
                    KT.preload.put(KT.upper(partId), display);
                }
            }
        }
    }

    static int registerSet(String id, ConfigurationSection s) {
        boolean fullSet = s.getBoolean("fullSet", false);
        ItemGroup g = KT.group(s.getString("item_group"));
        if (g == null) {
            MissingItems.record("物品组缺失:" + s.getString("item_group"));
            return 0;
        }

        List<ProtectionType> types = new ArrayList<>();
        for (String raw : s.getStringList("protection_types")) {
            if (raw == null || raw.isBlank()) continue;
            Pair<ExceptionHandler.HandleResult, ProtectionType> r =
                ExceptionHandler.handleEnumValueOf("盔甲套 " + id + " 错误的盔甲保护类型: " + raw,
                    ProtectionType.class, raw.trim().toUpperCase(Locale.ROOT));
            if (r.getFirstValue() == ExceptionHandler.HandleResult.FAILED) return 0;
            types.add(r.getSecondValue());
        }

        int n = 0;
        for (String part : CHECKS) {
            ConfigurationSection p = s.getConfigurationSection(part);
            if (p == null) continue;
            String partId = KT.upper(partId(id, s, p));

            ItemStack display = KT.preload.get(partId);
            if (display == null) display = Read.item(p, false);
            if (display == null) {
                KT.log("盔甲套 " + id + " 的 " + part + ": 无展示物品，跳过该部件");
                continue;
            }
            SlimefunItemStack sfis = new SlimefunItemStack(partId, display);

            RecipeType rt = RecipeTypes.resolve(p.getString("recipe_type",
                s.getString("recipe_type", "NULL")));
            ItemStack[] recipe = Read.recipe(p.getConfigurationSection("recipe"), 9);

            List<PotionEffect> effects = new ArrayList<>();
            for (String line : p.getStringList("potion_effects")) {
                if (line == null || line.isBlank()) continue;
                String[] kv = line.trim().split(" ");
                if (kv.length != 2) {
                    KT.log("盔甲套 " + id + " 的 " + part + ": 错误的药水效果格式: " + line);
                    continue;
                }
                PotionEffectType type = PlayerEffects.potionType(kv[0]);
                if (type == null) {
                    KT.log("盔甲套 " + id + " 的 " + part + ": 错误的药水效果类型: " + kv[0]);
                    continue;
                }
                int level;
                try {
                    level = Integer.parseInt(kv[1]);
                } catch (NumberFormatException e) {
                    KT.log("盔甲套 " + id + " 的 " + part + ": 错误的药水效果等级: " + line);
                    continue;
                }
                if (level < 0) {
                    KT.log("盔甲套 " + id + " 的 " + part + ": 药水效果等级不能为负数: " + line);
                    continue;
                }
                effects.add(new PotionEffect(type, effectDurationTicks(), level));
            }

            // 注意：CustomArmorPiece 构造器内部已自行 register()，此处不能再调一次，
            // 否则 Slimefun 会抛 IdConflictException（同一 id 注册两遍，第二次直接失败）。
            new CustomArmorPiece(g, sfis, rt, recipe, effects.toArray(new PotionEffect[0]),
                fullSet, id, types.toArray(new ProtectionType[0]), "Komutech");
            n++;
        }

        if (n == 0) {
            KT.log("盔甲套 " + id + ": 没有找到任何盔甲部分");
        }
        return n;
    }

    /** 部件 id：优先 id_alias / 段内 id，否则 <套id>_<部件大写>（与原 addon.getId 一致）。 */
    private static String partId(String setId, ConfigurationSection set, ConfigurationSection part) {
        String alias = set.getString("id_alias", part.getString("id", ""));
        if (alias != null && !alias.isBlank()) return alias;
        return setId + "_" + part.getName().toUpperCase(Locale.ROOT);
    }

    private static int effectDurationTicks() {
        int interval = Slimefun.getCfg().getInt("options.armor-update-interval");
        return (Math.max(interval, 20) + 180) * 20;
    }
}
