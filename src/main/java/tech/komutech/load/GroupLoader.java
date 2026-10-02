package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.LockedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SeasonalItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import java.time.Month;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.slimefun.AdvancedNestedItemGroup;
import tech.komutech.objects.slimefun.ItemGroupButton;

/**
 * 加载 groups.yml：nested / sub / button / seasonal / locked / normal。
 *
 * <p>先注册 nested 根组再注册子组（SubItemGroup 构造需要父 NestedItemGroup）。
 * 逐条 try/catch 故障隔离：单条坏数据只跳过该组，不会连累后续全部物品。
 *
 * <p><b>按钮组必须用 {@link AdvancedNestedItemGroup} 作父组</b>：原实现里
 * {@code createButtonGroup} 要求父组 {@code instanceof AdvancedNestedItemGroup}，否则报错并返回
 * null（按钮组整条丢失）。AdvancedNestedItemGroup 覆写了 {@code setup}，点击子组时会先判断
 * {@code instanceof ItemGroupButton} 并执行其 {@code actions}（link / console /
 * open_itemgroup / display_slimefunitem / script），而不是打开空物品组。
 * 本服 groups.yml 共 37 个 button 组，因此根组必须用它，不能用原生 NestedItemGroup。
 */
public final class GroupLoader {

    private GroupLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(KT.plugin, "groups.yml");
        // 第一遍：nested 根组
        for (String key : y.getKeys(false)) {
            try {
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                String type = s.getString("type", "normal").toLowerCase(Locale.ROOT);
                if (type.equals("nested") || type.equals("parent")) {
                    ItemStack display = Read.item(s.getConfigurationSection("item"), false);
                    if (display == null) {
                        KT.log("groups " + key + ": 无展示物品");
                        continue;
                    }
                    AdvancedNestedItemGroup g = new AdvancedNestedItemGroup(nsKey(key), display, s.getInt("tier", 3));
                    g.register(KT.plugin);
                    KT.groups.put(key.toLowerCase(Locale.ROOT), g);
                }
            } catch (Exception e) {
                KT.log("groups " + key + " 注册失败，跳过: " + e);
            }
        }
        int roots = KT.groups.size();
        // 第二遍：其余
        for (String key : y.getKeys(false)) {
            try {
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                String type = s.getString("type", "normal").toLowerCase(Locale.ROOT);
                if (type.equals("nested") || type.equals("parent")) continue;
                ItemStack display = Read.item(s.getConfigurationSection("item"), false);
                if (display == null) {
                    KT.log("groups " + key + ": 无展示物品");
                    continue;
                }
                registerChild(key, s, type, display);
            } catch (Exception e) {
                KT.log("groups " + key + " 注册失败，跳过: " + e);
            }
        }
        KT.plugin.getLogger().info("groups.yml: 注册 " + (KT.groups.size() - roots) + " 子组，共 " + KT.groups.size());
    }

    private static void registerChild(String key, ConfigurationSection s, String type, ItemStack display) {
        int tier = s.getInt("tier", 3);
        ItemGroup parent = KT.group(s.getString("parent"));
        switch (type) {
            case "seasonal": {
                int month = s.getInt("month", 1);
                SeasonalItemGroup g = new SeasonalItemGroup(nsKey(key),
                    Month.of(Math.max(1, Math.min(12, month))), tier, display);
                g.register(KT.plugin);
                KT.groups.put(key.toLowerCase(Locale.ROOT), g);
                break;
            }
            case "locked": {
                LockedItemGroup g = new LockedItemGroup(nsKey(key), display, tier, new NamespacedKey[0]);
                g.register(KT.plugin);
                KT.groups.put(key.toLowerCase(Locale.ROOT), g);
                break;
            }
            case "button": {
                // 父组必须是 AdvancedNestedItemGroup，否则按钮的 actions 永远不会被执行
                if (parent instanceof AdvancedNestedItemGroup advanced) {
                    ItemGroupButton g = new ItemGroupButton(nsKey(key), advanced, display, tier,
                        s.getStringList("actions"));
                    g.register(KT.plugin);
                    KT.groups.put(key.toLowerCase(Locale.ROOT), g);
                } else {
                    KT.log("groups " + key + ": 父组 " + s.getString("parent") + " 不是嵌套物品组，按钮组降级为普通子组");
                    registerSub(key, parent, display, tier);
                }
                break;
            }
            case "sub":
            default: {
                registerSub(key, parent, display, tier);
                break;
            }
        }
    }

    private static void registerSub(String key, ItemGroup parent, ItemStack display, int tier) {
        if (parent instanceof NestedItemGroup nested) {
            SubItemGroup g = new SubItemGroup(nsKey(key), nested, display, tier);
            g.register(KT.plugin);
            KT.groups.put(key.toLowerCase(Locale.ROOT), g);
        } else {
            ItemGroup g = new ItemGroup(nsKey(key), display, tier);
            g.register(KT.plugin);
            KT.groups.put(key.toLowerCase(Locale.ROOT), g);
        }
    }

    private static NamespacedKey nsKey(String key) {
        return new NamespacedKey(KT.plugin, key.toLowerCase(Locale.ROOT));
    }
}
