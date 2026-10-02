package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.generators.SolarGenerator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

public class CustomSolarGenerator extends SolarGenerator {
   private final int lightLevel;

   public CustomSolarGenerator(ItemGroup var1, int var2, int var3, SlimefunItemStack var4, RecipeType var5, ItemStack[] var6, int var7, int var8) {
      super(var1, var2, var3, var4, var5, var6, var7);
      if (var8 > 15 || var8 < 0) {
         var8 = 15;
      }

      this.lightLevel = var8;
      this.register(KT.plugin());
   }

   public int getGeneratedOutput(Location var1, SlimefunBlockData var2) {
      World var3 = var1.getWorld();
      if (var3.getEnvironment() != Environment.NORMAL) {
         return 0;
      } else {
         boolean var4 = this.isDaytime(var3);
         if (!var4 && this.getNightEnergy() < 1) {
            return 0;
         } else if (var3.isChunkLoaded(var1.getBlockX() >> 4, var1.getBlockZ() >> 4)
            && var1.getBlock().getRelative(0, 1, 0).getLightFromSky() >= (byte)this.lightLevel) {
            return var4 ? this.getDayEnergy() : this.getNightEnergy();
         } else {
            return 0;
         }
      }
   }

   private boolean isDaytime(World var1) {
      long var2 = var1.getTime();
      return !var1.hasStorm() && !var1.isThundering() && (var2 < 12300L || var2 > 23850L);
   }
}
