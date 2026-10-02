package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.LinkedOutput;
import tech.komutech.objects.customs.machine.CustomWorkbench;
import tech.komutech.objects.machine.CustomLinkedMachineRecipe;
import tech.komutech.script.ScriptEval;

/**
 * 加载 workbenches.yml（工作台）。
 *
 * <p>复刻原 {@code WorkbenchReader}：capacity>=0、energyPerCraft>0、click 必须在 0..53，
 * 工作配方用「联动槽位」模型（输入/输出都按 {@code slot} 定位到具体格子），
 * 与原实现一致地构造 {@link CustomLinkedMachineRecipe}。
 */
public final class WorkbenchesLoader {

    private WorkbenchesLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "workbenches.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log("workbenches.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("workbenches.yml: 注册 " + ok + ", 跳过 " + skip);
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

        String rtName = s.getString("recipe_type", "NULL");
        int size = "infinity_forge".equalsIgnoreCase(rtName) ? 36 : 9;
        RecipeType rt = RecipeTypes.resolve(rtName);
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), size);

        List<Integer> input = s.getIntegerList("input");
        List<Integer> output = s.getIntegerList("output");
        int capacity = s.getInt("capacity");
        if (capacity < 0) {
            KT.log("workbenches.yml " + id + " 能源容量 < 0，跳过");
            return false;
        }
        int energyPerCraft = s.getInt("energyPerCraft");
        if (energyPerCraft <= 0) {
            KT.log("workbenches.yml " + id + " 单次能耗 <= 0，跳过");
            return false;
        }
        ScriptEval eval = ScriptLoader.load(s.getString("script"), "工作台", id);
        int click = s.getInt("click", -1);
        if (click < 0 || click > 53) {
            KT.log("workbenches.yml " + id + " 点击槽位非法，跳过");
            return false;
        }
        boolean hideAllRecipes = s.getBoolean("hideAllRecipes", false);

        List<CustomLinkedMachineRecipe> recipes =
            readRecipes(id, input.size(), output.size(), s.getConfigurationSection("recipes"));

        new CustomWorkbench(
            g, sfis, rt, recipe,
            input.stream().mapToInt(Integer::intValue).toArray(),
            output.stream().mapToInt(Integer::intValue).toArray(),
            recipes, energyPerCraft, capacity, KT.menus.get(effId), hideAllRecipes, click, eval
        ); // 构造器内部已自行 register()，勿重复调用
        return true;
    }

    /** 联动槽位配方解析：与原 WorkbenchReader.readRecipes 逐条对应。 */
    static List<CustomLinkedMachineRecipe> readRecipes(String machineId, int inSize, int outSize,
                                                       ConfigurationSection recipes) {
        List<CustomLinkedMachineRecipe> list = new ArrayList<>();
        if (recipes == null) return list;
        for (String key : recipes.getKeys(false)) {
            ConfigurationSection r = recipes.getConfigurationSection(key);
            if (r == null) continue;
            ConfigurationSection inSec = r.getConfigurationSection("input");
            if (inSec == null) {
                KT.log("工作台 " + machineId + " 工作配方 " + key + " 没有输入物品，跳过");
                continue;
            }
            ConfigurationSection outSec = r.getConfigurationSection("output");
            if (outSec == null) {
                KT.log("工作台 " + machineId + " 工作配方 " + key + " 没有输出物品，跳过");
                continue;
            }

            List<ItemStack> freeOutput = new ArrayList<>();
            List<Integer> freeChances = new ArrayList<>();
            Map<Integer, ItemStack> linkedOutput = new HashMap<>();
            Map<Integer, Integer> linkedChances = new HashMap<>();
            for (int i = 0; i < outSize; i++) {
                ConfigurationSection slot = outSec.getConfigurationSection(String.valueOf(i + 1));
                ItemStack item = Read.item(slot, true);
                if (item != null && item.getType() != Material.AIR) {
                    int chance = slot.getInt("chance", 100);
                    if (chance < 1) {
                        KT.log("工作台 " + machineId + " 工作配方 " + key + " 概率 < 1，已转为 1");
                        chance = 1;
                    }
                    int slotIdx = slot.getInt("slot", -1);
                    if (slotIdx == -1) {
                        freeOutput.add(item);
                        freeChances.add(chance);
                    } else {
                        linkedOutput.put(slotIdx, item);
                        linkedChances.put(slotIdx, chance);
                    }
                }
            }

            boolean chooseOne = r.getBoolean("chooseOne", false);
            boolean forDisplay = r.getBoolean("forDisplay", false);
            boolean hide = r.getBoolean("hide", false);
            Set<Integer> noConsumes = new HashSet<>();
            Map<Integer, ItemStack> linkedInput = new HashMap<>();
            for (int i = 0; i < inSize; i++) {
                ConfigurationSection slot = inSec.getConfigurationSection(String.valueOf(i + 1));
                if (slot == null) continue;
                ItemStack item = Read.item(slot, true);
                if (item != null) {
                    int slotIdx = slot.getInt("slot", -1);
                    if (slotIdx == -1) {
                        KT.log("工作台 " + machineId + " 工作配方 " + key + " 输入槽位为空");
                    } else if (slotIdx >= 0 && slotIdx <= 53) {
                        linkedInput.put(slotIdx, item);
                        if (slot.getBoolean("noConsume", false)) {
                            noConsumes.add(slotIdx);
                        }
                    } else {
                        KT.log("工作台 " + machineId + " 工作配方 " + key + " 输入槽位超出范围");
                    }
                }
            }

            // 原实现此处生成的是 [0,1,2,...] 的索引序列而非概率值（沿用其既有行为）
            int[] freeChancesSeq = new int[freeChances.size()];
            for (int i = 0; i < freeChancesSeq.length; i++) {
                freeChancesSeq[i] = i;
            }

            list.add(new CustomLinkedMachineRecipe(
                0,
                linkedInput,
                new LinkedOutput(freeOutput.toArray(new ItemStack[0]), linkedOutput, freeChancesSeq, linkedChances),
                chooseOne, forDisplay, hide, noConsumes
            ));
        }
        return list;
    }
}
