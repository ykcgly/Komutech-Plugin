package tech.komutech.native_scripts.support;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public final class DisplayReflectionHelper {
   private DisplayReflectionHelper() {
   }

   public static BlockDisplay spawnBlockDisplay(Location var0, BlockData var1) {
      return spawnBlockDisplay(var0, var1, 2.0F);
   }

   public static BlockDisplay spawnBlockDisplay(Location var0, BlockData var1, float var2) {
      return (BlockDisplay)var0.getWorld().spawn(var0, BlockDisplay.class, var2x -> {
         var2x.setBlock(var1);
         var2x.setGlowing(true);
         var2x.setBrightness(new Brightness(15, 15));
         var2x.setViewRange(100.0F);
         var2x.setGravity(false);
         var2x.setInvulnerable(true);
         applyScale(var2x, var2, var2, var2);
      });
   }

   public static ItemDisplay spawnItemDisplay(Location var0, ItemStack var1, float var2) {
      return (ItemDisplay)var0.getWorld().spawn(var0, ItemDisplay.class, var2x -> {
         var2x.setItemStack(var1);
         var2x.setGlowing(true);
         var2x.setBrightness(new Brightness(15, 15));
         var2x.setViewRange(100.0F);
         var2x.setGravity(false);
         var2x.setInvulnerable(true);
         applyScale(var2x, var2, var2, var2);
      });
   }

   public static TextDisplay spawnTextDisplay(Location var0, String var1, float var2) {
      return (TextDisplay)var0.getWorld().spawn(var0, TextDisplay.class, var2x -> {
         var2x.setText(var1);
         var2x.setSeeThrough(true);
         var2x.setDefaultBackground(false);
         var2x.setBillboard(Billboard.CENTER);
         var2x.setViewRange(50.0F);
         var2x.setGravity(false);
         var2x.setInvulnerable(true);
         applyScale(var2x, var2, var2, var2);
      });
   }

   public static void applyScale(Display var0, float var1, float var2, float var3) {
      try {
         Class var4 = Class.forName("org.joml.Vector3f");
         Class var5 = Class.forName("org.joml.AxisAngle4f");
         Object var6 = var4.getConstructor(float.class, float.class, float.class).newInstance(0.0F, 0.0F, 0.0F);
         Object var7 = var5.getConstructor(float.class, float.class, float.class, float.class).newInstance(0.0F, 0.0F, 1.0F, 0.0F);
         Object var8 = var4.getConstructor(float.class, float.class, float.class).newInstance(var1, var2, var3);
         Object var9 = var5.getConstructor(float.class, float.class, float.class, float.class).newInstance(0.0F, 0.0F, 1.0F, 0.0F);
         Transformation var10 = Transformation.class.getConstructor(var4, var5, var4, var5).newInstance(var6, var7, var8, var9);
         var0.setTransformation(var10);
      } catch (ReflectiveOperationException var11) {
      }
   }

   public static BlockData parseBlockData(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.contains(":") ? var0.substring(var0.indexOf(58) + 1) : var0;
         Material var2 = Material.matchMaterial(var1.toUpperCase());
         if (var2 == null) {
            var2 = Material.HORN_CORAL_FAN;
         }

         return var2.createBlockData();
      } else {
         return Material.HORN_CORAL_FAN.createBlockData();
      }
   }
}
