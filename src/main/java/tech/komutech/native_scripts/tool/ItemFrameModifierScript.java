package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class ItemFrameModifierScript implements NativeScript {
   private static final String COMPRESSED_ID = "KOMUTECH_JZ_GJ_压缩展示框修改器";
   private static final String MODE_MARKER = "§6§l当前模式:";

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      if (!AdminToolsSupport.checkCooldown(var2, 1000L)) {
         return null;
      } else if (var2.isSneaking()) {
         toggleMode(var3, var2);
         return null;
      } else {
         ItemFrame var4 = (ItemFrame)AdminToolsSupport.rayTraceEntity(var2, 5.0, var0 -> var0 instanceof ItemFrame);
         if (var4 == null) {
            return null;
         } else if (!AdminToolsSupport.hasPermission(var2, var4.getLocation())) {
            KomutechSupport.send(var2, "§c你没有权限修改这个展示框！");
            var2.playSound(var2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
            return null;
         } else {
            boolean var5 = isShowMode(var3);
            SlimefunItem var6 = SlimefunItem.getByItem(var3);
            if (var6 != null && "KOMUTECH_JZ_GJ_压缩展示框修改器".equals(var6.getId())) {
               ArrayList<ItemFrame> var7 = new ArrayList<>();
               var7.add(var4);
               BlockFace var8 = var4.getFacing();

               for (Entity var10 : EntityQueries.entities(var4, 1.5)) {
                  if (var10 instanceof ItemFrame var11 && var11.getFacing() == var8) {
                     int var12 = Math.abs(var11.getLocation().getBlockX() - var4.getLocation().getBlockX());
                     int var13 = Math.abs(var11.getLocation().getBlockY() - var4.getLocation().getBlockY());
                     int var14 = Math.abs(var11.getLocation().getBlockZ() - var4.getLocation().getBlockZ());
                     if (var12 <= 1 && var13 <= 1 && var14 <= 1 && AdminToolsSupport.hasPermission(var2, var11.getLocation())) {
                        var7.add(var11);
                     }
                  }
               }

               for (ItemFrame var16 : var7) {
                  var16.setVisible(var5);
               }

               KomutechSupport.actionBar(var2, (var5 ? "显形" : "隐形") + " §7(影响了 " + var7.size() + " 个展示框)");
            } else {
               var4.setVisible(var5);
               KomutechSupport.actionBar(var2, "§a已" + (var5 ? "显形" : "隐形") + "！");
            }

            var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
            return null;
         }
      }
   }

   private static void toggleMode(ItemStack var0, Player var1) {
      ItemMeta var2 = var0.getItemMeta();
      if (var2 != null) {
         ArrayList<String> var3 = var2.hasLore() ? new ArrayList<>(var2.getLore()) : new ArrayList<>();
         boolean var4 = isShowMode(var0);
         var3.removeIf(var0x -> var0x != null && var0x.contains("§6§l当前模式:"));
         var3.add(var4 ? "§6§l当前模式: §7隐形" : "§6§l当前模式: §b显形");
         var2.setLore(var3);
         var0.setItemMeta(var2);
         KomutechSupport.actionBar(var1, "§a已切换至: " + (var4 ? "隐形" : "显形"));
      }
   }

   private static boolean isShowMode(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      if (var1 != null && var1.hasLore()) {
         for (String var3 : var1.getLore()) {
            if (var3 != null && var3.contains("§b显形")) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
