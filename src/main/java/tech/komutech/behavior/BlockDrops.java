package tech.komutech.behavior;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

/**
 * 方块破坏掉落（等价原 DropFromBlock + BlockListener 的 drop_from / drop_chance / drop_amount 链路）。
 *
 * <p>注册期由 ItemsLoader 调用 {@link #add} 登记「破坏某方块 → 概率掉落某物品」，
 * 运行期在 {@code BlockBreakEvent} 里按概率掷点并掉落。原实现的掉落判定同样挂在
 * BlockBreakEvent，故只要能触发该事件（含玄铁镐走 Player#breakBlock 破坏传送门）掉落即生效。
 */
public final class BlockDrops implements Listener {

    private BlockDrops() {}

    private static final class Drop {
        final String itemId;
        final int chance;
        final int min;
        final int max;

        Drop(String itemId, int chance, int min, int max) {
            this.itemId = itemId;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }
    }

    private static final Map<Material, List<Drop>> TABLE = new EnumMap<>(Material.class);

    public static void add(Material block, String itemId, int chance, int min, int max) {
        TABLE.computeIfAbsent(block, k -> new ArrayList<>()).add(new Drop(itemId, chance, min, max));
    }

    /**
     * 解析 drop_amount（支持 "1" 或 "1-3" 区间），返回 {min,max}，由每次掉落时掷点。
     * 与原 ItemsLoader/ItemReader 的解析规则一致。
     */
    public static int[] parseAmountRange(String value) {
        if (value != null) {
            int dash = value.indexOf('-');
            if (dash > 0) {
                try {
                    int lo = Integer.parseInt(value.substring(0, dash).trim());
                    int hi = Integer.parseInt(value.substring(dash + 1).trim());
                    if (lo >= 1 && hi >= lo) return new int[]{lo, hi};
                } catch (NumberFormatException ignored) {
                }
            }
            try {
                int v = Integer.parseInt(value.trim());
                if (v >= 1) return new int[]{v, v};
            } catch (NumberFormatException ignored) {
            }
        }
        return new int[]{1, 1};
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        List<Drop> drops = TABLE.get(e.getBlock().getType());
        if (drops == null || drops.isEmpty()) return;
        for (Drop d : drops) {
            if (ThreadLocalRandom.current().nextInt(100) >= d.chance) continue;
            SlimefunItem sf = SlimefunItem.getById(d.itemId);
            if (sf == null) continue;
            ItemStack stack = sf.getItem().clone();
            int amount = d.min >= d.max ? d.min : ThreadLocalRandom.current().nextInt(d.min, d.max + 1);
            stack.setAmount(amount);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5), stack);
        }
    }

    public static void register(org.bukkit.plugin.Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new BlockDrops(), plugin);
    }

    /** 供日志/自检使用：已登记的掉落来源方块数。 */
    public static int size() {
        return TABLE.size();
    }
}
