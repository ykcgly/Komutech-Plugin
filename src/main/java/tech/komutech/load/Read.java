package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import tech.komutech.KT;
import tech.komutech.util.colors.CMIChatColor;

/**
 * 共享读取器：把一个物品段解析为 {@link ItemStack}，把配方段解析为 {@code ItemStack[]}。
 *
 * <p>行为与原 {@code CommonUtils.readItem/readRecipe} 逐一对应（这是「不改变物品行为」的关键）：
 * 材质自动识别（ey/ew→skull、http→skull_url、64位hex→skull_hash）、{@code |} 备选材质、
 * name/lore 经 CMIChatColor 解析（支持 {@code {#RRGGBB}} 与 {@code &} 码）、glow 用海之眷顾+
 * 隐藏附魔实现、modelId→CustomModelData、附魔列表、皮革/药水/烟花染色、材质版本别名映射。
 *
 * <p>新增 {@code serialized} 类型：读取内嵌的 Bukkit 序列化 ItemStack（由 saveditem 内联展开而来），
 * 等价于原实现「先取模板堆、再用本段 name/lore 覆盖」的 RSCItemStack 语义。
 */
public final class Read {

    private Read() {}

    private static final Pattern HEX64 = Pattern.compile("^[0-9A-Fa-f]{64}$");

    /** 材质版本别名（与原 CommonUtils.materialMappings 一致）。 */
    private static final Map<String, String> MATERIAL_MAPPINGS = Map.of(
        "GRASS", "SHORT_GRASS",
        "SHORT_GRASS", "GRASS",
        "SCUTE", "TURTLE_SCUTE",
        "TURTLE_SCUTE", "SCUTE"
    );

    private static final Map<String, PlayerSkin> HASH_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> BASE64_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> URL_SKINS = new HashMap<>();

    /** 读取物品段。{@code countable=true} 时应用 amount。 */
    public static ItemStack item(ConfigurationSection s, boolean countable) {
        if (s == null) return null;
        String type = s.getString("material_type", "mc");
        if (!type.equalsIgnoreCase("none") && !s.contains("material")) {
            KT.log("设置了材料类型但没有设置材料，跳过");
            return null;
        }
        String material = s.getString("material", "");
        List<String> lore = CMIChatColor.translate(s.getStringList("lore"));
        String name = CMIChatColor.translate(s.getString("name", ""));
        boolean glow = s.getBoolean("glow", false);
        int modelId = s.getInt("modelId");
        int amount = s.getInt("amount", 1);

        // "|" 备选材质：按顺序尝试，取第一个可解析的（与原实现一致）
        if (material.contains("|")) {
            for (String alt : material.split("\\|")) {
                ItemStack r = resolve(s, type, alt.trim(), name, lore);
                if (r != null) return finish(r, s, countable, amount, modelId, glow);
            }
            KT.log("无法找到物品 " + material + "，已跳过");
            return null;
        }
        ItemStack stack = resolve(s, type, material.trim(), name, lore);
        if (stack == null) return null;
        return finish(stack, s, countable, amount, modelId, glow);
    }

    private static ItemStack finish(ItemStack stack, ConfigurationSection s, boolean countable,
                                    int amount, int modelId, boolean glow) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && modelId > 0) meta.setCustomModelData(modelId);
        if (countable && amount > 0) stack.setAmount(Math.min(amount, 64));
        if (meta != null) stack.setItemMeta(meta);
        if (glow) glow(stack);
        applyEnchantments(stack, s);
        return stack;
    }

    private static ItemStack resolve(ConfigurationSection s, String type, String material,
                                     String name, List<String> lore) {
        String t = type;
        if (material.startsWith("ey") || material.startsWith("ew")) t = "skull";
        else if (material.toLowerCase(Locale.ROOT).startsWith("http")) t = "skull_url";
        else if (HEX64.matcher(material).matches()) t = "skull_hash";

        switch (t.toLowerCase(Locale.ROOT)) {
            case "none":
                return new ItemStack(Material.AIR);
            case "skull_hash": {
                PlayerSkin skin = HASH_SKINS.computeIfAbsent(material, PlayerSkin::fromHashCode);
                return apply(PlayerHead.getItemStack(skin), name, lore);
            }
            case "skull_base64":
            case "skull": {
                PlayerSkin skin = BASE64_SKINS.computeIfAbsent(material, PlayerSkin::fromBase64);
                return apply(PlayerHead.getItemStack(skin), name, lore);
            }
            case "skull_url": {
                PlayerSkin skin = URL_SKINS.computeIfAbsent(material, PlayerSkin::fromURL);
                return apply(PlayerHead.getItemStack(skin), name, lore);
            }
            case "slimefun": {
                String id = KT.upper(material);
                ItemStack pre = KT.preload.get(id);
                if (pre != null) return apply(pre.clone(), name, lore);
                SlimefunItem sf = SlimefunItem.getById(id);
                if (sf != null) return apply(sf.getItem().clone(), name, lore);
                // 前向引用（目标尚未注册）：包一层补上 id PDC，等价于注册后的堆
                if (pre != null) return new SlimefunItemStack(id, pre);
                MissingItems.record(id);
                return new CustomItemStack(Material.STONE, name, lore);
            }
            case "serialized": {
                // Bukkit 的 YamlConstructor 会对「任意层级」含 "==" 键的映射做反序列化，
                // 所以 serialized 段在 loadFromStream 时就已经变成真正的 ItemStack 对象了，
                // getConfigurationSection("serialized") 会返回 null。必须先按对象取值。
                Object raw = s.get("serialized");
                if (raw instanceof ItemStack already) {
                    return apply(already.clone(), name, lore);
                }
                ConfigurationSection sec = s.getConfigurationSection("serialized");
                if (sec == null) {
                    MissingItems.record("序列化段缺失");
                    return new CustomItemStack(Material.STONE, name, lore);
                }
                ItemStack base;
                try {
                    base = ItemStack.deserialize(sec.getValues(true));
                } catch (Exception e) {
                    KT.log("反序列化 serialized 段失败: " + e);
                    return new CustomItemStack(Material.STONE, name, lore);
                }
                return apply(base, name, lore);
            }
            default: {
                Material m = Material.matchMaterial(material);
                if (m == null) {
                    String alias = MATERIAL_MAPPINGS.get(material.toUpperCase(Locale.ROOT));
                    if (alias != null) m = Material.matchMaterial(alias);
                }
                if (m == null) {
                    MissingItems.record("材质 " + material);
                    m = Material.STONE;
                }
                ItemStack stack = new ItemStack(m);
                applyColor(stack, s.getString("color"));
                return apply(stack, name, lore);
            }
        }
    }

    /** 覆盖显示名/lore（非空才覆盖，等价于原 RSCItemStack 语义）。 */
    private static ItemStack apply(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        if (name != null && !name.isBlank()) meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /** glow：海之眷顾 + 隐藏附魔（与原 CommonUtils.doGlow 一致）。 */
    private static void glow(ItemStack stack) {
        Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
        if (e != null) stack.addUnsafeEnchantment(e, 1);
        stack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private static void applyEnchantments(ItemStack stack, ConfigurationSection s) {
        if (!s.contains("enchantments") || !s.isList("enchantments")) return;
        for (String line : s.getStringList("enchantments")) {
            String[] parts = line.split(" ");
            if (parts.length != 2) {
                KT.log("附魔格式错误，跳过: " + line);
                continue;
            }
            Enchantment en = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase(Locale.ROOT)));
            if (en == null) {
                KT.log("未知附魔: " + parts[0]);
                continue;
            }
            try {
                stack.addUnsafeEnchantment(en, Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
                KT.log("附魔等级错误: " + line);
            }
        }
    }

    /** 应用 "R,G,B" 颜色到皮革护甲/药水/烟花。 */
    private static void applyColor(ItemStack stack, String color) {
        if (color == null || color.isEmpty()) return;
        String[] rgb = color.split(",");
        if (rgb.length != 3) return;
        try {
            Color c = Color.fromRGB(Integer.parseInt(rgb[0].trim()),
                Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof LeatherArmorMeta leather) leather.setColor(c);
            else if (meta instanceof PotionMeta potion) {
                potion.setColor(c);
                potion.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            } else if (meta instanceof FireworkEffectMeta firework) {
                firework.setEffect(org.bukkit.FireworkEffect.builder().withColor(c).build());
                firework.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            } else return;
            stack.setItemMeta(meta);
        } catch (RuntimeException ignored) {
            KT.log("颜色格式错误: " + color);
        }
    }

    /** 读取配方段，槽位键 "1".."size"，产出长度为 size 的数组（空槽为 null）。 */
    public static ItemStack[] recipe(ConfigurationSection recipeSec, int size) {
        ItemStack[] out = new ItemStack[size];
        if (recipeSec == null) return out;
        for (int i = 0; i < size; i++) {
            ConfigurationSection slot = recipeSec.getConfigurationSection(String.valueOf(i + 1));
            if (slot != null) out[i] = item(slot, true);
        }
        return out;
    }

    public static void clearSkinCache() {
        HASH_SKINS.clear();
        BASE64_SKINS.clear();
        URL_SKINS.clear();
    }
}
