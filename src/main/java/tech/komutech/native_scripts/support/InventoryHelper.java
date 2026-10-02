package tech.komutech.native_scripts.support;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tech.komutech.native_scripts.SlimefunBridge;

public final class InventoryHelper {
   private InventoryHelper() {
   }

   public static void giveOrDrop(Player var0, ItemStack var1, String var2) {
      if (var0 != null && var1 != null && !var1.getType().isAir()) {
         PlayerInventory var3 = var0.getInventory();
         if (var3.firstEmpty() == -1) {
            var0.getWorld().dropItemNaturally(var0.getLocation(), var1);
            if (var2 != null) {
               ChatColors.send(var0, var2);
            }
         } else {
            var3.addItem(new ItemStack[]{var1});
         }
      }
   }

   public static void giveOrDrop(Player var0, String var1, String var2) {
      giveOrDrop(var0, SlimefunBridge.getItemStackById(var1), var2);
   }

   public static boolean canAdd(Player var0, ItemStack var1) {
      return var0 != null && var1 != null && !var1.getType().isAir()
         ? var0.getInventory().firstEmpty() != -1 || var0.getInventory().containsAtLeast(var1, 1)
         : false;
   }
}
