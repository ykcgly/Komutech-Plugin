package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.machine.CustomEnergyGenerator;
import tech.komutech.objects.customs.machine.CustomMachine;
import tech.komutech.objects.customs.machine.CustomNoEnergyMachine;
import tech.komutech.objects.customs.parent.AbstractEmptyMachine;
import tech.komutech.objects.machine.MachineRecord;
import tech.komutech.script.ScriptEval;

/**
 * 加载 machines.yml（通用机器）。
 *
 * <p>严格复刻原 {@code MachineReader.readEach} 的分支：
 * <ul>
 *   <li>有 {@code energy} 段 → 按 {@code energyOutput} 是否存在分为
 *       {@link CustomEnergyGenerator} / {@link CustomMachine}；容量或类型非法时降级为无电机器；</li>
 *   <li>无 {@code energy} 段 → {@link CustomNoEnergyMachine}，{@code work} 支持单值或列表；</li>
 * </ul>
 * 注册后调用 {@code register(plugin)}，与原实现一致。
 */
public final class MachinesLoader {

    private MachinesLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "machines.yml");
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
                KT.log("machines.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        for (int i = 0; i < late.size(); i++) {
            try {
                if (register(lateIds.get(i), late.get(i))) ok++; else skip++;
            } catch (Exception e) {
                KT.log("machines.yml " + lateIds.get(i) + "(lateInit) 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("machines.yml: 注册 " + ok + ", 跳过 " + skip);
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

        ScriptEval eval = ScriptLoader.load(s.getString("script"), "机器", id);
        List<Integer> input = s.getIntegerList("input");
        List<Integer> output = s.getIntegerList("output");
        CustomMenu menu = KT.menus.get(effId);

        AbstractEmptyMachine<?> machine;
        if (s.contains("energy")) {
            ConfigurationSection energy = s.getConfigurationSection("energy");
            if (energy == null) {
                KT.log("机器 " + id + " 能源段读取失败，降级为无电机器");
                machine = new CustomNoEnergyMachine(g, sfis, rt, recipe, menu, input, output, eval, -1);
            } else {
                int capacity = energy.getInt("capacity");
                if (capacity < 0) {
                    KT.log("机器 " + id + " 能源容量 < 0，降级为无电机器");
                    machine = new CustomNoEnergyMachine(g, sfis, rt, recipe, menu, input, output, eval, -1);
                } else {
                    EnergyNetComponentType type = energyType(energy.getString("type"));
                    if (type == null) {
                        KT.log("机器 " + id + " 能源类型非法，降级为无电机器");
                        machine = new CustomNoEnergyMachine(g, sfis, rt, recipe, menu, input, output, eval, -1);
                    } else {
                        MachineRecord record = new MachineRecord(capacity);
                        if (energy.contains("energyOutput")) {
                            int out = energy.getInt("energyOutput");
                            if (out < 0) {
                                KT.log("发电机 " + id + " 能量输出 < 0，降级为普通有电机器");
                                machine = new CustomMachine(g, sfis, rt, recipe, menu, input, output, record, type, eval);
                            } else {
                                machine = new CustomEnergyGenerator(g, sfis, rt, recipe, menu, input, output, record, type, eval, out);
                            }
                        } else {
                            machine = new CustomMachine(g, sfis, rt, recipe, menu, input, output, record, type, eval);
                        }
                    }
                }
            }
        } else {
            List<Integer> work = new ArrayList<>();
            if (s.isInt("work")) {
                work.add(s.getInt("work", -1));
            } else if (s.isList("work")) {
                work.addAll(s.getIntegerList("work"));
            }
            machine = new CustomNoEnergyMachine(g, sfis, rt, recipe, menu, input, output, eval, work);
        }

        machine.register(KT.plugin);
        return true;
    }

    private static EnergyNetComponentType energyType(String raw) {
        if (raw == null) return null;
        try {
            return EnergyNetComponentType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // 原实现用 ExceptionHandler.handleEnumValueOf 报同名错误后降级，这里同语义
            return null;
        }
    }
}
