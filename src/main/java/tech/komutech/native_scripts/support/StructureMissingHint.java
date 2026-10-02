package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tech.komutech.native_scripts.support.MachineScriptHelper;

/**
 * 多方块机器"缺失方块"提示。
 *
 * <p>点击核心的启动按钮检测到结构不完整时，会在每一个缺失的位置生成一套展示实体：
 * 一个<strong>红色发光</strong>的物品展示（大小与投影一致：scale 0.5）+ 上方 0.8 格的名称牌，
 * 同时在聊天栏给出<strong>世界绝对坐标</strong>与原版方块的<strong>中文名</strong>（由客户端翻译 {@link Material#getBlockTranslationKey()}）。
 * 玩家把正确的方块放好后，对应提示会自动消失。
 */
public final class StructureMissingHint {
   private static final NamespacedKey HINT_KEY = new NamespacedKey("KomutechNative".toLowerCase(), "structure_missing_hint");
   private static final int MAX_MESSAGE_LINES = 12;
   private static final float BODY_SCALE = 0.5F;
   private static final double LABEL_HEIGHT = 0.8;
   private static final double SCAN_RADIUS = 24.0;
   /** 粘液方块的数据可能在放置后隔几 tick 才写入，这里延迟再核对一次。 */
   private static final long SFID_RECHECK_DELAY = 3L;
   private static final Map<String, Map<String, StructureMissingHint.Hint>> ACTIVE = new ConcurrentHashMap<>();
   private static final Map<String, Set<String>> INDEX = new ConcurrentHashMap<>();
   private static final Map<String, UUID> OWNERS = new ConcurrentHashMap<>();
   private static volatile boolean listenerRegistered = false;

   private StructureMissingHint() {
   }

   /** 一条缺失记录：location 为<strong>世界绝对坐标</strong>（已按方向旋转过）。 */
   public record MissingBlock(Location location, Material material, String sfId) {
   }

   /**
    * 展示缺失提示。会先清掉该核心上一次的提示，保证不会叠加。
    *
    * @param missing 缺失清单，包含绝对位置、需要的原版材质、需要的粘液物品 ID（可为 null）
    */
   public static void showMissing(Player var0, Location var1, List<MissingBlock> var2) {
      ensureListener();
      clear(var1);
      if (var0 == null || var1 == null || var2 == null || var2.isEmpty()) {
         return;
      }

      String var3 = coreKey(var1);
      Map<String, StructureMissingHint.Hint> var4 = new ConcurrentHashMap<>();
      List<MissingBlock> var5 = new ArrayList<>();

      for (MissingBlock var7 : var2) {
         Location var8 = var7.location();
         if (var8 != null && var8.getWorld() != null) {
            String var9 = blockKey(var8);
            StructureMissingHint.Hint var10 = spawnHint(var3, var8, var7.material(), var7.sfId());
            if (var10 != null) {
               var4.put(var9, var10);
               INDEX.computeIfAbsent(var9, var0x -> ConcurrentHashMap.newKeySet()).add(var3);
               var5.add(var7);
            }
         }
      }

      if (!var4.isEmpty()) {
         ACTIVE.put(var3, var4);
         OWNERS.put(var3, var0.getUniqueId());
         notifyMissing(var0, var5);
      }
   }

   /** 清除某个核心的全部缺失提示（重新检测、激活成功、核心被拆掉、切换方向时都要调用）。 */
   public static void clear(Location var0) {
      if (var0 == null) {
         return;
      }

      String var1 = coreKey(var0);
      Map<String, StructureMissingHint.Hint> var2 = ACTIVE.remove(var1);
      if (var2 != null) {
         for (Map.Entry<String, StructureMissingHint.Hint> var4 : var2.entrySet()) {
            removeDisplays(var4.getValue());
            unindex((String)var4.getKey(), var1);
         }
      }

      OWNERS.remove(var1);
      removeTaggedEntities(var0);
   }

   private static StructureMissingHint.Hint spawnHint(String var0, Location var1, Material var2, String var3) {
      World var4 = var1.getWorld();
      if (var4 == null) {
         return null;
      } else {
         Location var5 = new Location(var4, (double)var1.getBlockX() + 0.5, (double)var1.getBlockY() + 0.5, (double)var1.getBlockZ() + 0.5);
         ItemStack var6 = displayStack(var2, var3);
         Component var7 = nameComponent(var2, var3);
         ArrayList<Display> var8 = new ArrayList<>(2);

         try {
            ItemDisplay var9 = (ItemDisplay)var4.spawn(var5, ItemDisplay.class, var2x -> {
               var2x.setItemStack(var6);
               var2x.setGlowing(true);
               var2x.setGlowColorOverride(Color.RED);
               var2x.setBrightness(new Display.Brightness(15, 15));
               var2x.setViewRange(100.0F);
               var2x.setGravity(false);
               var2x.setInvulnerable(true);
               var2x.setPersistent(false);
               DisplayReflectionHelper.applyScale(var2x, 0.5F, 0.5F, 0.5F);
            });
            tag(var9, var0);
            var8.add(var9);
            TextDisplay var10 = (TextDisplay)var4.spawn(var5.clone().add(0.0, 0.8, 0.0), TextDisplay.class, var2x -> {
               var2x.text(var7);
               var2x.setSeeThrough(true);
               var2x.setDefaultBackground(false);
               var2x.setBillboard(Display.Billboard.CENTER);
               var2x.setViewRange(50.0F);
               var2x.setGravity(false);
               var2x.setInvulnerable(true);
               var2x.setPersistent(false);
               DisplayReflectionHelper.applyScale(var2x, 1.0F, 1.0F, 1.0F);
            });
            tag(var10, var0);
            var8.add(var10);
            return new StructureMissingHint.Hint(var2, var3, var8);
         } catch (Throwable var11) {
            for (Display var13 : var8) {
               if (!var13.isDead()) {
                  var13.remove();
               }
            }

            return null;
         }
      }
   }

   private static void ensureListener() {
      if (!listenerRegistered) {
         Plugin var0 = KomutechSupport.plugin();
         if (var0 == null) {
            return;
         }

         Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlace(BlockPlaceEvent var1x) {
               StructureMissingHint.handlePlace(var1x.getBlock());
            }
         }, var0);
         listenerRegistered = true;
      }
   }

   private static void handlePlace(Block var0) {
      if (var0 == null || INDEX.isEmpty()) {
         return;
      } else {
         String var1 = blockKey(var0.getLocation());
         Set<String> var2 = INDEX.get(var1);
         if (var2 != null && !var2.isEmpty()) {
            for (String var4 : new ArrayList<>(var2)) {
               Map<String, StructureMissingHint.Hint> var5 = ACTIVE.get(var4);
               if (var5 == null) {
                  INDEX.remove(var1);
                  return;
               }

               StructureMissingHint.Hint var6 = var5.get(var1);
               if (var6 == null) {
                  var2.remove(var4);
               } else if (matches(var0, var6)) {
                  resolveHint(var4, var1, var0);
               } else if (var6.sfId() != null && var0.getType() == var6.material()) {
                  // 材质对了但粘液物品数据还没落盘，几 tick 后再核对一次
                  delayRecheck(var4, var1, var0);
               }
            }

            if (var2.isEmpty()) {
               INDEX.remove(var1);
            }
         }
      }
   }

   private static void delayRecheck(String var0, String var1, Block var2) {
      Plugin var3 = KomutechSupport.plugin();
      if (var3 != null) {
         BukkitTask var4 = Bukkit.getScheduler().runTaskLater(var3, () -> {
            Block var5x = var2.getWorld().getBlockAt(var2.getX(), var2.getY(), var2.getZ());
            Map<String, StructureMissingHint.Hint> var6 = ACTIVE.get(var0);
            if (var6 != null) {
               StructureMissingHint.Hint var7 = var6.get(var1);
               if (var7 != null && matches(var5x, var7)) {
                  resolveHint(var0, var1, var5x);
               }
            }
         }, 3L);
      }
   }

   /** 玩家放对了方块：移除该处的提示；如果整台机器都补齐了，给个提示。 */
   private static void resolveHint(String var0, String var1, Block var2) {
      Map<String, StructureMissingHint.Hint> var3 = ACTIVE.get(var0);
      if (var3 != null) {
         StructureMissingHint.Hint var4 = var3.remove(var1);
         if (var4 != null) {
            removeDisplays(var4);
         }

         unindex(var1, var0);
         if (var3.isEmpty()) {
            ACTIVE.remove(var0);
            UUID var5 = OWNERS.remove(var0);
            notifyCompleted(var5);
         }
      }
   }

   private static boolean matches(Block var0, StructureMissingHint.Hint var1) {
      if (var0.getType() != var1.material()) {
         return false;
      } else if (var1.sfId() == null) {
         return true;
      } else {
         SlimefunItem var2 = MachineScriptHelper.getSfItem(var0.getLocation());
         String var3 = var2 == null ? null : var2.getId();
         return var3 != null && var3.equalsIgnoreCase(var1.sfId());
      }
   }

   private static void notifyMissing(Player var0, List<MissingBlock> var1) {
      KomutechSupport.send(var0, "§c结构检测未完成：" + var1.size() + " 处缺失，已在对应位置生成红色投影，放对方块后会自动消失。");
      int var2 = Math.min(var1.size(), 12);

      for (int var3 = 0; var3 < var2; var3++) {
         MissingBlock var4 = var1.get(var3);
         Location var5 = var4.location();
         Component var6 = LegacyComponentSerializer.legacyAmpersand()
            .deserialize("§c● §7需放置 §f")
            .append(nameComponent(var4.material(), var4.sfId()))
            .append(LegacyComponentSerializer.legacyAmpersand().deserialize(" §7坐标 §f" + formatCoords(var5)));
         var0.sendMessage(var6);
      }

      if (var1.size() > var2) {
         KomutechSupport.send(var0, "§7  … 其余 " + (var1.size() - var2) + " 处已用红色投影标出");
      }
   }

   private static void notifyCompleted(UUID var0) {
      if (var0 != null) {
         Player var1 = Bukkit.getPlayer(var0);
         if (var1 != null) {
            KomutechSupport.send(var1, "§a缺失方块已全部补齐，可以再次点击启动了。");
         }
      }
   }

   /** 世界坐标，不再输出相对核心的偏移。 */
   private static String formatCoords(Location var0) {
      return var0.getWorld() == null
         ? var0.getBlockX() + " " + var0.getBlockY() + " " + var0.getBlockZ()
         : var0.getWorld().getName() + " §8/§f " + var0.getBlockX() + " " + var0.getBlockY() + " " + var0.getBlockZ();
   }

   /**
    * 名称：粘液物品用它的显示名；原版方块走 {@link Material#getBlockTranslationKey()}，
    * 交给<strong>客户端</strong>翻译成玩家自己的语言（中文客户端显示中文）。
    */
   public static Component nameComponent(Material var0, String var1) {
      SlimefunItem var2 = var1 == null ? null : resolveSlimefunItem(var1);
      if (var2 != null) {
         // 粘液物品的显示名可能用 & 也可能用 §，统一成 & 再交给 legacy 解析
         return LegacyComponentSerializer.legacyAmpersand().deserialize(plainText(var2.getItemName()).replace('§', '&'));
      } else if (var0 != null) {
         return Component.translatable(var0.isBlock() ? var0.getBlockTranslationKey() : var0.getItemTranslationKey());
      } else {
         return Component.text("未知方块");
      }
   }

   private static ItemStack displayStack(Material var0, String var1) {
      SlimefunItem var2 = var1 == null ? null : resolveSlimefunItem(var1);
      if (var2 != null) {
         return var2.getItem().clone();
      } else if (var0 != null && var0.isItem()) {
         return new ItemStack(var0);
      } else {
         return new ItemStack(Material.BARRIER);
      }
   }

   private static SlimefunItem resolveSlimefunItem(String var0) {
      SlimefunItem var1 = SlimefunItem.getById(var0);
      if (var1 != null) {
         return var1;
      } else {
         for (SlimefunItem var3 : Slimefun.getRegistry().getAllSlimefunItems()) {
            if (var3 != null && var3.getId() != null && var3.getId().equalsIgnoreCase(var0)) {
               return var3;
            }
         }

         return null;
      }
   }

   /** 显示名里可能带着颜色符号以外的内容，这里只做去空处理，颜色照原样交给 legacy 解析。 */
   private static String plainText(String var0) {
      return var0 == null ? "未知物品" : (var0.isBlank() ? "未知物品" : var0);
   }

   private static void removeDisplays(StructureMissingHint.Hint var0) {
      if (var0 != null) {
         for (Display var2 : var0.displays()) {
            if (var2 != null && !var2.isDead()) {
               var2.remove();
            }
         }
      }
   }

   /** 兜底：清理没有被内存记录到的标记实体（例如服务器重启后的残留）。 */
   private static void removeTaggedEntities(Location var0) {
      if (var0 != null && var0.getWorld() != null) {
         String var1 = coreKey(var0);

         for (Entity var3 : (Collection<Entity>)EntityQueries.entities(var0.getWorld(), var0, 24.0)) {
            if (var3 instanceof Display && var1.equals(readTag(var3)) && !var3.isDead()) {
               var3.remove();
            }
         }
      }
   }

   private static void tag(Display var0, String var1) {
      var0.getPersistentDataContainer().set(HINT_KEY, PersistentDataType.STRING, var1);
   }

   private static String readTag(Entity var0) {
      return (String)var0.getPersistentDataContainer().get(HINT_KEY, PersistentDataType.STRING);
   }

   private static void unindex(String var0, String var1) {
      Set<String> var2 = INDEX.get(var0);
      if (var2 != null) {
         var2.remove(var1);
         if (var2.isEmpty()) {
            INDEX.remove(var0);
         }
      }
   }

   private static String coreKey(Location var0) {
      String var10000 = var0.getWorld() == null ? "null" : var0.getWorld().getName();
      return var10000 + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   private static String blockKey(Location var0) {
      String var10000 = var0.getWorld() == null ? "null" : var0.getWorld().getName();
      return var10000 + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   private static final class Hint {
      private final Material material;
      private final String sfId;
      private final List<Display> displays;

      private Hint(Material var1, String var2, List<Display> var3) {
         this.material = var1;
         this.sfId = var2;
         this.displays = var3;
      }

      Material material() {
         return this.material;
      }

      String sfId() {
         return this.sfId;
      }

      List<Display> displays() {
         return this.displays;
      }
   }
}
