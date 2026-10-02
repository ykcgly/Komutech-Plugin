package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.machine.CustomRecipeMachine;
import tech.komutech.objects.machine.CustomMachineRecipe;
import tech.komutech.util.CommonUtils;

/**
 * 加载 recipe_machines.yml（配方机器）。
 *
 * <p>复刻原 {@code RecipeMachineReader}：校验 输入/输出槽非空、capacity>=0、
 * energyPerCraft>0、speed>0，然后构造 {@link CustomRecipeMachine}。
 * 工作配方的解析（含 chance、chooseOne、forDisplay、hide、noConsume）见
 * {@link #readRecipes}，与原实现逐条对应。
 */
public final class RecipeMachinesLoader {

    private RecipeMachinesLoader() {}

    public static void load() {
        loadFile("recipe_machines.yml");
        // linked_recipe_machines.yml 走 CustomLinkedRecipeMachine（下一阶段接入）
    }

    static void loadFile(String file) {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, file);
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(file, id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log(file + " " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info(file + ": 注册 " + ok + ", 跳过 " + skip);
    }

    static boolean register(String file, String id, ConfigurationSection s) {
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
        if (input.isEmpty()) {
            KT.log(file + " " + id + " 输入槽为空，跳过");
            return false;
        }
        if (output.isEmpty()) {
            KT.log(file + " " + id + " 输出槽为空，跳过");
            return false;
        }
        int capacity = s.getInt("capacity");
        if (capacity < 0) {
            KT.log(file + " " + id + " 能源容量 < 0，跳过");
            return false;
        }
        int energyPerCraft = s.getInt("energyPerCraft");
        if (energyPerCraft <= 0) {
            KT.log(file + " " + id + " 单次能耗 <= 0，跳过");
            return false;
        }
        int speed = s.getInt("speed");
        if (speed <= 0) {
            KT.log(file + " " + id + " 合成速度 <= 0，跳过");
            return false;
        }
        boolean hideAllRecipes = s.getBoolean("hideAllRecipes", false);

        List<CustomMachineRecipe> recipes =
            readRecipes(id, input.size(), output.size(), s.getConfigurationSection("recipes"));

        new CustomRecipeMachine(
            g, sfis, rt, recipe,
            input.stream().mapToInt(Integer::intValue).toArray(),
            output.stream().mapToInt(Integer::intValue).toArray(),
            recipes, energyPerCraft, capacity, KT.menus.get(effId), speed, hideAllRecipes
        ); // 构造器内部已自行 register()，勿重复调用
        return true;
    }

    /** 工作配方解析：与原 RecipeMachineReader.readRecipes + addToList 一致。 */
    static List<CustomMachineRecipe> readRecipes(String machineId, int inSize, int outSize,
                                                 ConfigurationSection recipes) {
        List<CustomMachineRecipe> list = new ArrayList<>();
        if (recipes == null) return list;
        for (String key : recipes.getKeys(false)) {
            ConfigurationSection r = recipes.getConfigurationSection(key);
            if (r == null) continue;
            int seconds = r.getInt("seconds");
            if (seconds < 0) {
                KT.log("配方机器 " + machineId + " 的工作配方 " + key + " 间隔时间未设置或 < 0，跳过");
                continue;
            }
            // 原实现直接把可能为 null 的 input 交给 readRecipe（得到空输入），不跳过整条配方。
            // 模板机里存在「只有 output + forDisplay」的展示配方，跳过会让它们整条丢失。
            ConfigurationSection inSec = r.getConfigurationSection("input");
            ItemStack[] inputs = Read.recipe(inSec, inSize);
            ConfigurationSection outSec = r.getConfigurationSection("output");
            if (outSec == null) {
                KT.log("配方机器 " + machineId + " 的工作配方 " + key + " 没有输出物品，跳过");
                continue;
            }
            List<Integer> chances = new ArrayList<>();
            ItemStack[] outputs = new ItemStack[outSize];
            for (int i = 0; i < outSize; i++) {
                ConfigurationSection slot = outSec.getConfigurationSection(String.valueOf(i + 1));
                ItemStack item = Read.item(slot, true);
                if (item != null) {
                    int chance = slot.getInt("chance", 100);
                    if (chance < 1) {
                        KT.log("配方机器 " + machineId + " 工作配方 " + key + " 概率 < 1，已转为 1");
                        chance = 1;
                    }
                    outputs[i] = item;
                    chances.add(chance);
                }
            }
            // noConsume：逐槽标记为不消耗；整体 noConsume 时覆盖为全部槽位
            IntList noConsume = new IntArrayList();
            List<String> slotKeys =
                inSec == null ? new ArrayList<>() : new ArrayList<>(inSec.getKeys(false));
            for (String sk : slotKeys) {
                ConfigurationSection slot = inSec.getConfigurationSection(sk);
                if (slot != null && slot.getBoolean("noConsume", false)) {
                    noConsume.add(slotKeys.indexOf(sk));
                }
            }
            if (r.getBoolean("noConsume", false)) {
                noConsume.clear();
                noConsume.addAll(IntStream.rangeClosed(0, slotKeys.size()).boxed().toList());
            }
            list.add(new CustomMachineRecipe(
                seconds,
                CommonUtils.removeNulls(inputs),
                CommonUtils.removeNulls(outputs),
                chances,
                r.getBoolean("chooseOne", false),
                r.getBoolean("forDisplay", false),
                r.getBoolean("hide", false),
                noConsume
            ));
        }
        return list;
    }
}
