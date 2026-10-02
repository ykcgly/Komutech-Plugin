package tech.komutech.native_scripts.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class RegionItemFrameScript implements NativeScript {
   private static final String RECORDER_ID = "KOMUTECH_JZ_GJ_方位记录器";
   private static final NamespacedKey MODE_KEY = new NamespacedKey("komutech", "fwzskxgq_mode");
   private static final int BATCH_SIZE = 20;

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      final Player var2 = var1.player();
      ItemStack var3 = var1.item();
      if (!AdminToolsSupport.checkCooldown(var2, 1000L)) {
         return null;
      } else if (var2.isSneaking()) {
         cycleMode(var3, var2);
         return null;
      } else {
         ItemStack var4 = AdminToolsSupport.findInventoryItem(var2, "KOMUTECH_JZ_GJ_方位记录器");
         if (var4 == null) {
            KomutechSupport.send(var2, "§c背包中没有「方位记录器」");
            return null;
         } else {
            ItemMeta var5 = var4.getItemMeta();
            if (var5 != null && var5.hasLore()) {
               List var6 = var5.getLore();
               AdminToolsSupport.BlockPos var7 = AdminToolsSupport.parsePos(var6, "§6§l位置一：");
               AdminToolsSupport.BlockPos var8 = AdminToolsSupport.parsePos(var6, "§6§l位置二：");
               if (var7 != null && var8 != null) {
                  int var9 = Math.min(var7.x(), var8.x());
                  int var10 = Math.max(var7.x(), var8.x());
                  int var11 = Math.min(var7.y(), var8.y());
                  int var12 = Math.max(var7.y(), var8.y());
                  int var13 = Math.min(var7.z(), var8.z());
                  int var14 = Math.max(var7.z(), var8.z());
                  final RegionItemFrameScript.FrameMode var15 = RegionItemFrameScript.FrameMode.fromId(readMode(var3));
                  final List var16 = collectFrames(var2.getWorld(), var9, var11, var13, var10, var12, var14);
                  if (var16.isEmpty()) {
                     KomutechSupport.send(var2, "§e区域内无展示框");
                     return null;
                  } else {
                     Plugin var17 = KomutechSupport.plugin();
                     if (var17 == null) {
                        return null;
                     } else {
                        KomutechSupport.actionBar(var2, "§a开始" + var15.label + "区域内展示框...");
                        var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                        (new BukkitRunnable() {
                           int index = 0;
                           int processed = 0;

                           public void run() {
                              if (!var2.isOnline()) {
                                 this.cancel();
                              } else {
                                 for (int var1x = 0; this.index < var16.size() && var1x < 20; var1x++) {
                                    ItemFrame var2x = (ItemFrame)var16.get(this.index++);
                                    if (var2x.isValid() && AdminToolsSupport.hasPermission(var2, var2x.getLocation())) {
                                       var15.apply(var2x);
                                       this.processed++;
                                    }
                                 }

                                 if (this.index >= var16.size()) {
                                    this.cancel();
                                    KomutechSupport.actionBar(var2, "§a操作完成，共处理 " + this.processed + " 个展示框");
                                    var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                                 }
                              }
                           }
                        }).runTaskTimer(var17, 0L, 2L);
                        return null;
                     }
                  }
               } else {
                  KomutechSupport.send(var2, "§c坐标信息不完整");
                  return null;
               }
            } else {
               KomutechSupport.send(var2, "§c记录器上没有坐标信息");
               return null;
            }
         }
      }
   }

   private static List<ItemFrame> collectFrames(World var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      Location var7 = new Location(var0, (var1 + var4) / 2.0, (var2 + var5) / 2.0, (var3 + var6) / 2.0);
      double var8 = Math.max(var4 - var1, Math.max(var5 - var2, var6 - var3)) / 2.0 + 1.0;
      ArrayList var10 = new ArrayList();

      for (Entity var12 : EntityQueries.entities(var0, var7, var8)) {
         if (var12 instanceof ItemFrame var13) {
            Location var14 = var13.getLocation();
            if (var14.getBlockX() >= var1
               && var14.getBlockX() <= var4
               && var14.getBlockY() >= var2
               && var14.getBlockY() <= var5
               && var14.getBlockZ() >= var3
               && var14.getBlockZ() <= var6) {
               var10.add(var13);
            }
         }
      }

      return var10;
   }

   private static int readMode(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 == null) {
         return 0;
      } else {
         PersistentDataContainer var2 = var1.getPersistentDataContainer();
         Integer var3 = (Integer)var2.get(MODE_KEY, PersistentDataType.INTEGER);
         return var3 == null ? 0 : Math.floorMod(var3, RegionItemFrameScript.FrameMode.values().length);
      }
   }

   private static void cycleMode(ItemStack var0, Player var1) {
      int var2 = (readMode(var0) + 1) % RegionItemFrameScript.FrameMode.values().length;
      ItemMeta var3 = var0.getItemMeta();
      if (var3 != null) {
         var3.getPersistentDataContainer().set(MODE_KEY, PersistentDataType.INTEGER, var2);
         ArrayList<String> var4 = var3.hasLore() ? new ArrayList<>(var3.getLore()) : new ArrayList<>();
         var4.removeIf(var0x -> var0x != null && var0x.startsWith("§6§l当前模式:"));
         var4.add(RegionItemFrameScript.FrameMode.values()[var2].loreLine);
         var3.setLore(var4);
         var0.setItemMeta(var3);
         KomutechSupport.actionBar(var1, "§a已切换至: " + RegionItemFrameScript.FrameMode.values()[var2].label);
         var1.playSound(var1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
      }
   }

   private static enum FrameMode {
      HIDE("隐形", "§6§l当前模式: §7隐形", var0 -> var0.setVisible(false)),
      SHOW("显形", "§6§l当前模式: §b显形", var0 -> var0.setVisible(true)),
      REMOVE("拆除", "§6§l当前模式: §c拆除", Entity::remove);

      final String label;
      final String loreLine;
      private final Consumer<ItemFrame> action;

      private FrameMode(String nullxx, String nullxxx, Consumer<ItemFrame> nullxxxx) {
         this.label = nullxx;
         this.loreLine = nullxxx;
         this.action = nullxxxx;
      }

      static RegionItemFrameScript.FrameMode fromId(int var0) {
         RegionItemFrameScript.FrameMode[] var1 = values();
         return var1[Math.floorMod(var0, var1.length)];
      }

      void apply(ItemFrame var1) {
         this.action.accept(var1);
      }
   }
}
