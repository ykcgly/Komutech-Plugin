package tech.komutech.util;

import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

public final class MachineOutputHelper {
   private MachineOutputHelper() {
   }

   public static void pushOrDrop(@Nonnull BlockMenu var0, @Nonnull Block var1, @Nonnull ItemStack var2, int... var3) {
      if (!var2.getType().isAir() && var3 != null && var3.length != 0) {
         ItemStack var4 = var2.clone();
         int var5 = var4.getAmount();

         for (int var9 : var3) {
            if (var5 <= 0) {
               break;
            }

            ItemStack var10 = var0.getItemInSlot(var9);
            if (var10 != null && !var10.getType().isAir()) {
               if (var10.getAmount() < var10.getMaxStackSize() && StackUtils.itemsMatch(var4, var10, true, false)) {
                  int var14 = Math.min(var5, var10.getMaxStackSize() - var10.getAmount());
                  var10.setAmount(var10.getAmount() + var14);
                  var5 -= var14;
               }
            } else {
               int var11 = Math.min(var5, var4.getMaxStackSize());
               ItemStack var12 = var4.clone();
               var12.setAmount(var11);
               var0.replaceExistingItem(var9, var12);
               var5 -= var11;
            }
         }

         if (var5 > 0) {
            ItemStack var13 = var4.clone();
            var13.setAmount(var5);
            dropNaturally(var1, var13);
         }
      }
   }

   public static void dropNaturally(@Nonnull Block var0, @Nonnull ItemStack var1) {
      if (!var1.getType().isAir() && var1.getAmount() > 0) {
         World var2 = var0.getWorld();
         if (var2 != null) {
            Location var3 = var0.getLocation().add(0.5, 1.0, 0.5);
            ItemStack var4 = var1.clone();
            Runnable var5 = () -> {
               World var2x = var3.getWorld();
               if (var2x != null) {
                  var2x.dropItemNaturally(var3, var4);
               }
            };
            if (Bukkit.isPrimaryThread()) {
               var5.run();
            } else {
               Plugin var6 = KT.plugin();
               if (var6 != null && var6.isEnabled()) {
                  Bukkit.getScheduler().runTask(var6, var5);
               }
            }
         }
      }
   }
}
