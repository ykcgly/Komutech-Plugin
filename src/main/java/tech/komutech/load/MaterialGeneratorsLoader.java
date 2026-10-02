package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.machine.CustomMaterialGenerator;
import tech.komutech.util.CommonUtils;

/**
 * 加载 mat_generators.yml（材料生成器，本服内容量最大的一类：249 台）。
 *
 * <p>严格复刻原 {@code MaterialGeneratorReader.readEach}：
 * <ul>
 *   <li>菜单必须存在（{@code KT.menus} 按 upper(id) 查），否则整条跳过——这也是 menus.yml 里
 *       249 条「未被其它机器引用」的菜单的归属；</li>
 *   <li>{@code outputs} 段按 {@code output} 槽位数读配方数组，再 {@code removeNulls}；</li>
 *   <li>若 {@code outputs} 为空则退化到 {@code outputItem} 单输出 + {@code chance}；</li>
 *   <li>多输出时按 {@code outputs} 各子段的 {@code chance} 收集概率表；</li>
 *   <li>{@code tickRate} / {@code per} 必须 &gt;= 1，否则跳过；{@code status} 缺省 -1，
 *       存在时给该槽挂 {@code ChestMenuUtils.getEmptyClickHandler()}。</li>
 * </ul>
 */
public final class MaterialGeneratorsLoader {

    private MaterialGeneratorsLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "mat_generators.yml");
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
                KT.log("mat_generators.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        for (int i = 0; i < late.size(); i++) {
            try {
                if (register(lateIds.get(i), late.get(i))) ok++; else skip++;
            } catch (Exception e) {
                KT.log("mat_generators.yml " + lateIds.get(i) + "(lateInit) 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("mat_generators.yml: 注册 " + ok + ", 跳过 " + skip);
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

        RecipeType rt = RecipeTypes.resolve(s.getString("recipe_type", "NULL"));
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);

        CustomMenu menu = KT.menus.get(effId);
        if (menu == null) {
            KT.log("材料生成器 " + id + ": 对应菜单不存在，跳过");
            return false;
        }

        List<Integer> output = s.getIntegerList("output");
        int capacity = s.getInt("capacity", 0);
        ConfigurationSection outputs = s.getConfigurationSection("outputs");
        ItemStack[] raw = Read.recipe(outputs, output.size());
        List<Integer> chances = new ArrayList<>();
        boolean chooseOne = s.getBoolean("chooseOne", false);
        ItemStack[] nonNull = CommonUtils.removeNulls(raw);

        if (nonNull.length == 0) {
            ConfigurationSection outItem = s.getConfigurationSection("outputItem");
            ItemStack single = Read.item(outItem, true);
            if (single == null) {
                KT.log("材料生成器 " + id + ": 输出物品为空或格式错误，跳过");
                return false;
            }
            raw = new ItemStack[]{single};
            chances = List.of(outItem.getInt("chance", 100));
        } else if (raw.length > 1 && outputs != null) {
            for (String key : outputs.getKeys(false)) {
                ConfigurationSection sec = outputs.getConfigurationSection(key);
                if (Read.item(sec, true) != null) {
                    chances.add(sec.getInt("chance", 100));
                }
            }
        }

        raw = CommonUtils.removeNulls(raw);

        int tickRate = s.getInt("tickRate");
        if (tickRate < 1) {
            KT.log("材料生成器 " + id + ": tickRate 未设置或不能小于1，跳过");
            return false;
        }
        int per = s.getInt("per");
        if (per < 1) {
            KT.log("材料生成器 " + id + ": 单次生成能量花费(per)未设置或不能小于1，跳过");
            return false;
        }
        int status = s.contains("status") ? s.getInt("status") : -1;

        CustomMaterialGenerator gen = new CustomMaterialGenerator(
            g, sfis, rt, recipe, capacity, output, status, tickRate,
            Arrays.asList(raw), menu, per, chances, chooseOne
        );
        menu.addMenuClickHandler(status, ChestMenuUtils.getEmptyClickHandler());
        // 构造器内部已自行 register()，勿重复调用
        return true;
    }
}
