package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class SlimefunBlockPlacer {
   private SlimefunBlockPlacer() {
   }

   public static boolean place(Location var0, String var1) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         SlimefunItem var2 = SlimefunItem.getById(var1);
         if (var2 == null) {
            return false;
         } else {
            ItemStack var3 = var2.getItem();
            if (var3 != null && !var3.getType().isAir()) {
               Block var4 = var0.getBlock();
               var4.setType(var3.getType());
               if (!registerSlimefunBlock(var0, var1)) {
                  var4.setType(Material.AIR);
                  return false;
               } else {
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean placeVanilla(Location var0, String var1) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         Material var2 = Material.matchMaterial(var1);
         if (var2 == null) {
            try {
               var2 = Material.valueOf(var1.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var4) {
               return false;
            }
         }

         var0.getBlock().setType(var2);
         return true;
      } else {
         return false;
      }
   }

   public static boolean remove(Location var0) {
      if (var0 == null) {
         return false;
      } else {
         if (MachineScriptHelper.getSfItem(var0) != null) {
            try {
               Class var1 = Class.forName("io.github.thebusybiscuit.slimefun4.implementation.Slimefun");
               Object var2 = var1.getMethod("getDatabaseManager").invoke(null);
               Object var3 = var2.getClass().getMethod("getBlockDataController").invoke(var2);
               var3.getClass().getMethod("removeBlock", Location.class).invoke(var3, var0);
            } catch (ReflectiveOperationException var4) {
            }
         }

         Block var5 = var0.getBlock();
         if (!var5.getType().isAir()) {
            var5.setType(Material.AIR);
         }

         return true;
      }
   }

   private static boolean registerSlimefunBlock(Location var0, String var1) {
      try {
         Class var2 = Class.forName("io.github.thebusybiscuit.slimefun4.implementation.Slimefun");
         Object var3 = var2.getMethod("getDatabaseManager").invoke(null);
         Object var4 = var3.getClass().getMethod("getBlockDataController").invoke(var3);
         var4.getClass().getMethod("createBlock", Location.class, String.class).invoke(var4, var0, var1);
         return true;
      } catch (ReflectiveOperationException var5) {
         return false;
      }
   }
}
