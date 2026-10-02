package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class StructureProjectionHelper {
   private static final float PROJECTION_SCALE = 0.5F;

   private StructureProjectionHelper() {
   }

   public static StructureProjectionHelper.SpawnedProjection spawn(Location var0, String var1, String var2, ItemStack var3) {
      if (var3 != null) {
         return spawnAddonItem(var0, var3);
      } else {
         if (var2 != null) {
            SlimefunItem var4 = resolveSlimefunItem(var2);
            if (var4 != null) {
               return spawnAddonItem(var0, var4.getItem().clone());
            }
         }

         if (var1 != null && !var1.isBlank()) {
            Material var5 = Material.matchMaterial(var1.toUpperCase(Locale.ROOT));
            return var5 != null && var5.isItem()
               ? new StructureProjectionHelper.SpawnedProjection(DisplayReflectionHelper.spawnItemDisplay(var0, new ItemStack(var5), 0.5F), null)
               : null;
         } else {
            return null;
         }
      }
   }

   private static StructureProjectionHelper.SpawnedProjection spawnAddonItem(Location var0, ItemStack var1) {
      TextDisplay var2 = null;
      ItemMeta var3 = var1.getItemMeta();
      if (var3 != null && var3.hasDisplayName()) {
         var2 = DisplayReflectionHelper.spawnTextDisplay(var0.clone().add(0.0, 0.8, 0.0), var3.getDisplayName(), 1.0F);
      }

      return new StructureProjectionHelper.SpawnedProjection(DisplayReflectionHelper.spawnItemDisplay(var0, var1, 0.5F), var2);
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

   public record SpawnedProjection(Display body, Display label) {
   }
}
