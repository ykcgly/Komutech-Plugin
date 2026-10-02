package tech.komutech.objects.customs.machine.sf;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.AbstractGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class AdvancedAnimalGrowthAccelerator extends AbstractGrowthAccelerator {
   private static final ItemStack organicFood = ItemStackWrapper.wrap(SlimefunItems.ORGANIC_FOOD);
   private final int capacity;
   private final int radius;
   private final int energy_consumption;

   public AdvancedAnimalGrowthAccelerator(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, int var5, int var6, int var7) {
      super(var1, var2, var3, var4);
      this.capacity = var5;
      this.radius = var6;
      this.energy_consumption = var7;
   }

   protected void tick(Block var1) {
      BlockMenu var2 = StorageCacheUtils.getMenu(var1.getLocation());
      if (var2 != null) {
         for (Entity var4 : var1.getWorld().getNearbyEntities(var1.getLocation(), this.radius, this.radius, this.radius, this::isReadyToGrow)) {
            int[] var5 = this.getInputSlots();
            int var6 = var5.length;

            for (int var10 : var5) {
               if (SlimefunUtils.isItemSimilar(var2.getItemInSlot(var10), organicFood, false, false)) {
                  if (this.getCharge(var1.getLocation()) < this.energy_consumption) {
                     return;
                  }

                  Ageable var11 = (Ageable)var4;
                  this.removeCharge(var1.getLocation(), this.energy_consumption);
                  var2.consumeItem(var10);
                  var11.setAge(var11.getAge() + 2000);
                  if (var11.getAge() > 0) {
                     var11.setAge(0);
                  }

                  var4.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ((LivingEntity)var4).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                  return;
               }
            }
         }
      }
   }

   private boolean isReadyToGrow(Entity var1) {
      return !(var1 instanceof Ageable var2) ? false : var1.isValid() && !var2.isAdult();
   }

   public int getCapacity() {
      return this.capacity;
   }
}
