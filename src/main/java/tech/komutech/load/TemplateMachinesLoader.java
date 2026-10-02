package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.machine.CustomTemplateMachine;
import tech.komutech.objects.machine.MachineTemplate;

/**
 * 加载 template_machines.yml（模板机器：按放入的「模板物品」切换配方组）。
 *
 * <p>复刻原 {@code TemplateMachineReader}：必须找到菜单且菜单已设进度槽，
 * templateSlot 在 0..53，capacity>=0，consumption>0。
 */
public final class TemplateMachinesLoader {

    private TemplateMachinesLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "template_machines.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++; else skip++;
            } catch (Exception e) {
                KT.log("template_machines.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        KT.plugin.getLogger().info("template_machines.yml: 注册 " + ok + ", 跳过 " + skip);
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

        boolean fasterIfMore = s.getBoolean("fasterIfMoreTemplates", false);
        boolean moreOutputIfMore = s.getBoolean("moreOutputIfMoreTemplates", false);
        List<Integer> input = s.getIntegerList("input");
        List<Integer> output = s.getIntegerList("output");
        if (output.isEmpty()) {
            KT.log("template_machines.yml " + id + " 输出槽为空，跳过");
            return false;
        }
        CustomMenu menu = KT.menus.get(effId);
        if (menu == null) {
            KT.log("template_machines.yml " + id + " 未找到菜单，跳过");
            return false;
        }
        if (menu.getProgressSlot() < 0) {
            KT.log("template_machines.yml " + id + " 菜单进度槽未设置，跳过");
            return false;
        }

        int templateSlot = s.getInt("templateSlot");
        if (templateSlot < 0 || templateSlot >= 54) {
            KT.log("template_machines.yml " + id + " 模板槽位非法，跳过");
            return false;
        }
        int capacity = s.getInt("capacity");
        if (capacity < 0) {
            KT.log("template_machines.yml " + id + " 能源容量 < 0，跳过");
            return false;
        }
        int consumption = s.getInt("consumption");
        if (consumption <= 0) {
            KT.log("template_machines.yml " + id + " 消耗能量 <= 0，跳过");
            return false;
        }
        boolean hideAllRecipes = s.getBoolean("hideAllRecipes", false);

        List<MachineTemplate> templates =
            readTemplates(effId, input.size(), output.size(), s.getConfigurationSection("recipes"));

        new CustomTemplateMachine(
            g, sfis, rt, recipe, menu, input, output, templateSlot, templates,
            consumption, capacity, fasterIfMore, moreOutputIfMore, hideAllRecipes
        ); // 构造器内部已自行 register()，勿重复调用
        return true;
    }

    /** 模板组：键是模板物品 id，值是该模板下的配方列表。 */
    static List<MachineTemplate> readTemplates(String machineId, int inSize, int outSize,
                                               ConfigurationSection recipes) {
        List<MachineTemplate> list = new ArrayList<>();
        if (recipes == null) return list;
        for (String key : recipes.getKeys(false)) {
            ItemStack tpl = KT.preload.get(KT.upper(key));
            if (tpl == null) {
                SlimefunItem sf = SlimefunItem.getById(KT.upper(key));
                if (sf != null) tpl = sf.getItem().clone();
            }
            if (tpl == null) {
                KT.log("模板机器 " + machineId + " 找不到模板物品 " + key + "，跳过该模板");
                continue;
            }
            List<tech.komutech.objects.machine.CustomMachineRecipe> rs =
                RecipeMachinesLoader.readRecipes(machineId, inSize, outSize,
                    recipes.getConfigurationSection(key));
            list.add(new MachineTemplate(new SlimefunItemStack(KT.upper(key), tpl), rs));
        }
        return list;
    }
}
