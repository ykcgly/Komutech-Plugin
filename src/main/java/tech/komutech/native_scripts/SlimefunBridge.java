package tech.komutech.native_scripts;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.inventory.ItemStack;

public final class SlimefunBridge {
   private SlimefunBridge() {
   }

   public static String getItemId(ItemStack var0) {
      SlimefunItem var1 = getSlimefunItem(var0);
      return var1 == null ? null : var1.getId();
   }

   public static SlimefunItem getSlimefunItem(ItemStack var0) {
      return SlimefunItem.getByItem(var0);
   }

   public static SlimefunItem getSlimefunItemById(String var0) {
      return var0 != null && !var0.isEmpty() ? SlimefunItem.getById(var0) : null;
   }

   public static ItemStack getItemStackById(String var0) {
      SlimefunItem var1 = getSlimefunItemById(var0);
      if (var1 == null) {
         return null;
      } else {
         ItemStack var2 = var1.getItem().clone();
         var2.setAmount(1);
         return var2;
      }
   }

   public static ItemStack getSlimefunItemStack(ItemStack var0) {
      SlimefunItem var1 = getSlimefunItem(var0);
      return var1 == null ? null : var1.getItem().clone();
   }
}
