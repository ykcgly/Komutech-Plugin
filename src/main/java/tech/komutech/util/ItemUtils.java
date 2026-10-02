package tech.komutech.util;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashSet;
import org.bukkit.inventory.ItemStack;

public class ItemUtils {
   public static int getAllItemAmount(ItemStack... var0) {
      int var1 = 0;

      for (ItemStack var5 : var0) {
         if (var5 != null && !var5.getType().isAir()) {
            var1 += var5.getAmount();
         }
      }

      return var1;
   }

   public static int getAllItemTypeAmount(ItemStack... var0) {
      HashSet var1 = new HashSet();
      HashSet var2 = new HashSet();

      for (ItemStack var6 : var0) {
         if (var6 != null && !var6.getType().isAir()) {
            SlimefunItem var7 = SlimefunItem.getByItem(var6);
            if (var7 != null) {
               var1.add(var7);
            } else {
               var2.add(var6.getType());
            }
         }
      }

      return var1.size() + var2.size();
   }
}
