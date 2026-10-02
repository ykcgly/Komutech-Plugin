package tech.komutech.native_scripts.tool;

import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class DisplayCleanerScript implements NativeScript {
   private static final String MODE_MARKER = "§6§l当前模式:";
   private static final String SINGLE_LORE = "§6§l当前模式: §7单个清除";
   private static final String RANGE_LORE = "§6§l当前模式: §b范围清除";
   private static final double SINGLE_MAX_RADIUS = 32.0;
   private static final int SINGLE_CHUNK_RADIUS = 2;
   private static final NamespacedKey MEDITATION_DISPLAY_KEY = new NamespacedKey("KomutechNative".toLowerCase(), "pu_tuan_display");

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      if (!AdminToolsSupport.checkCooldown(var2, 1000L)) {
         return null;
      } else if (!AdminToolsSupport.hasPermission(var2, var2.getLocation())) {
         KomutechSupport.send(var2, "§c你没有权限在此区域清除展示物品！");
         var2.playSound(var2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
         return null;
      } else if (var2.isSneaking()) {
         AdminToolsSupport.switchLoreMode(var3, "§6§l当前模式:", "§6§l当前模式: §7单个清除", "§6§l当前模式: §b范围清除");
         ItemMeta var6 = var3.getItemMeta();
         boolean var7 = var6 != null && var6.hasLore() && var6.getLore().stream().anyMatch(var0 -> var0 != null && var0.contains("范围"));
         KomutechSupport.actionBar(var2, "§a已切换至: " + (var7 ? "范围清除" : "单个清除"));
         var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
         return null;
      } else {
         ItemMeta var4 = var3.getItemMeta();
         List var5 = var4 != null && var4.hasLore() ? var4.getLore() : List.of();
         if ("range".equals(AdminToolsSupport.loreMode(var5, "§6§l当前模式:", "single", "范围"))) {
            this.clearRange(var2);
         } else {
            this.clearSingle(var2);
         }

         return null;
      }
   }

   private void clearSingle(Player var1) {
      Display var2 = findNearestDisplay(var1);
      if (var2 == null) {
         KomutechSupport.send(var1, "§c附近没有全息物品");
      } else if (!AdminToolsSupport.hasPermission(var1, var2.getLocation())) {
         KomutechSupport.send(var1, "§c你没有权限移除这个全息物品！");
         var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
      } else {
         var2.remove();
         KomutechSupport.actionBar(var1, "§a已清除最近的全息物品及文字");
         var1.playSound(var1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
      }
   }

   private static Display findNearestDisplay(Player var0) {
      Location var1 = var0.getLocation();
      World var2 = var1.getWorld();
      if (var2 == null) {
         return null;
      } else {
         double var3 = 1024.0;
         Display var5 = null;
         double var6 = Double.MAX_VALUE;
         Chunk var8 = var1.getChunk();
         int var9 = var8.getX();
         int var10 = var8.getZ();

         for (int var11 = -2; var11 <= 2; var11++) {
            for (int var12 = -2; var12 <= 2; var12++) {
               if (var2.isChunkLoaded(var9 + var11, var10 + var12)) {
                  for (Entity var16 : var2.getChunkAt(var9 + var11, var10 + var12).getEntities()) {
                     if ((var16 instanceof ItemDisplay || var16 instanceof TextDisplay) && !var16.isDead() && !isProtectedDisplay(var16)) {
                        double var17 = var16.getLocation().distanceSquared(var1);
                        if (!(var17 > var3) && !(var17 >= var6)) {
                           var6 = var17;
                           var5 = (Display)var16;
                        }
                     }
                  }
               }
            }
         }

         return var5;
      }
   }

   private void clearRange(Player var1) {
      int var2 = 0;
      int var3 = 0;

      for (Entity var5 : EntityQueries.entities(var1.getLocation(), 10.0)) {
         if ((var5 instanceof ItemDisplay || var5 instanceof TextDisplay) && !var5.isDead() && !isProtectedDisplay(var5)) {
            if (AdminToolsSupport.hasPermission(var1, var5.getLocation())) {
               var5.remove();
               var2++;
            } else {
               var3++;
            }
         }
      }

      if (var2 > 0) {
         KomutechSupport.actionBar(var1, "§a已清除 " + var2 + " 个全息物品及文字" + (var3 > 0 ? "，跳过 " + var3 + " 个无权限的" : ""));
         var1.playSound(var1.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
      } else if (var3 > 0) {
         KomutechSupport.send(var1, "§c没有可清除的全息物品（无权限）");
         var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
      } else {
         KomutechSupport.send(var1, "§c范围内没有全息物品");
         var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
      }
   }

   private static boolean isProtectedDisplay(Entity var0) {
      return var0 instanceof Display var1 ? var1.getPersistentDataContainer().has(MEDITATION_DISPLAY_KEY, PersistentDataType.STRING) : false;
   }
}
