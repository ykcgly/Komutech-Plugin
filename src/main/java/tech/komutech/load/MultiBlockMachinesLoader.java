package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.machine.CustomMultiBlockMachine;
import tech.komutech.script.ScriptEval;

/**
 * 加载 mb_machines.yml（多方块机器）。
 *
 * <p>复刻原 {@code MultiBlockMachineReader}：放置配方（recipe 段）里必须含发射器，
 * {@code work} 指定的工作槽必须存在，工作配方为「输入九宫格 → 单输出」的映射。
 */
public final class MultiBlockMachinesLoader {

    private MultiBlockMachinesLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "mb_machines.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log("mb_machines.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("mb_machines.yml: 注册 " + ok + ", 跳过 " + skip);
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

        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);
        int work = s.getInt("work");
        if (work < 1) {
            KT.log("mb_machines.yml " + id + " 未设置工作槽，跳过");
            return false;
        }
        boolean hasDispenser = false;
        for (ItemStack it : recipe) {
            if (it != null && it.getType() == Material.DISPENSER) {
                hasDispenser = true;
                break;
            }
        }
        if (!hasDispenser) {
            KT.log("mb_machines.yml " + id + " 放置配方里没有发射器，跳过");
            return false;
        }
        if (recipe[work - 1] == null) {
            KT.log("mb_machines.yml " + id + " 对应工作方块不存在，跳过");
            return false;
        }

        Map<ItemStack[], ItemStack> recipes = readRecipes(id, s.getConfigurationSection("recipes"));
        SoundEffect sound = null;
        if (s.contains("sound")) {
            try {
                sound = SoundEffect.valueOf(s.getString("sound").toUpperCase());
            } catch (IllegalArgumentException ignored) {
                KT.log("mb_machines.yml " + id + " 声音类型非法: " + s.getString("sound"));
            }
        }
        ScriptEval eval = ScriptLoader.load(s.getString("script"), "多方块机器", id);

        // 构造器内部已自行 register()；MultiBlockMachine 覆写了 register() 并先 addItemHandler，
        // 重复调用会抛 UnsupportedOperationException（而不是 IdConflict）
        new CustomMultiBlockMachine(g, sfis, recipe, recipes, work, sound, eval);
        return true;
    }

    /** 工作配方：输入九宫格 → 单输出。 */
    static Map<ItemStack[], ItemStack> readRecipes(String machineId, ConfigurationSection recipes) {
        Map<ItemStack[], ItemStack> map = new HashMap<>();
        if (recipes == null) return map;
        for (String key : recipes.getKeys(false)) {
            ConfigurationSection r = recipes.getConfigurationSection(key);
            if (r == null) continue;
            ConfigurationSection inSec = r.getConfigurationSection("input");
            if (inSec == null) {
                KT.log("多方块机器 " + machineId + " 工作配方 " + key + " 没有输入物品，跳过");
                continue;
            }
            ItemStack[] inputs = Read.recipe(inSec, 9);
            ConfigurationSection outSec = r.getConfigurationSection("output");
            if (outSec == null) {
                KT.log("多方块机器 " + machineId + " 工作配方 " + key + " 没有输出物品，跳过");
                continue;
            }
            ItemStack output = Read.item(outSec, true);
            if (output == null) {
                KT.log("多方块机器 " + machineId + " 工作配方 " + key + " 输出物品为空，跳过");
                continue;
            }
            map.put(inputs, output);
        }
        return map;
    }
}
