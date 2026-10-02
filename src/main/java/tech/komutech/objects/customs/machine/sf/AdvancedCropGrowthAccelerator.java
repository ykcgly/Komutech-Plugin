package tech.komutech.objects.customs.machine.sf;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.CropGrowthAccelerator;
import org.bukkit.inventory.ItemStack;

public class AdvancedCropGrowthAccelerator extends CropGrowthAccelerator {
   private final int capacity;
   private final int radius;
   private final int energy_consumption;
   private final int speed;

   public AdvancedCropGrowthAccelerator(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, int var5, int var6, int var7, int var8) {
      super(var1, var2, var3, var4);
      this.capacity = var5;
      this.radius = var6;
      this.energy_consumption = var7;
      this.speed = var8;
   }

   public int getEnergyConsumption() {
      return this.energy_consumption;
   }

   public int getRadius() {
      return this.radius;
   }

   public int getSpeed() {
      return this.speed;
   }

   public int getCapacity() {
      return this.capacity;
   }
}
