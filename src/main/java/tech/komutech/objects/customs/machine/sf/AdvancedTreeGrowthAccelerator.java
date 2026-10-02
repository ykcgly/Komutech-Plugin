package tech.komutech.objects.customs.machine.sf;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.AbstractGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.inventory.ItemStack;

public class AdvancedTreeGrowthAccelerator extends AbstractGrowthAccelerator {
   private static final ItemStack organicFertilizer = ItemStackWrapper.wrap(SlimefunItems.FERTILIZER);
   private final int capacity;
   private final int radius;
   private final int energy_consumption;

   public AdvancedTreeGrowthAccelerator(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, int var5, int var6, int var7) {
      super(var1, var2, var3, var4);
      this.capacity = var5;
      this.radius = var6;
      this.energy_consumption = var7;
   }

   public int getCapacity() {
      return this.capacity;
   }

   protected void tick(@Nonnull Block var1) {
      BlockMenu var2 = StorageCacheUtils.getMenu(var1.getLocation());
      if (var2 != null && this.getCharge(var1.getLocation()) >= this.energy_consumption) {
         for (int var3 = -this.radius; var3 <= this.radius; var3++) {
            for (int var4 = -this.radius; var4 <= this.radius; var4++) {
               Block var5 = var1.getRelative(var3, 0, var4);
               if (Tag.SAPLINGS.isTagged(var5.getType()) && this.tryToBoostGrowth(var1, var2, var5)) {
                  return;
               }
            }
         }
      }
   }

   @ParametersAreNonnullByDefault
   private boolean tryToBoostGrowth(Block var1, BlockMenu var2, Block var3) {
      if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_17)) {
         return this.applyBoneMeal(var1, var3, var2);
      } else {
         Sapling var4 = (Sapling)var3.getBlockData();
         return var4.getStage() < var4.getMaximumStage() && this.updateSaplingData(var1, var3, var2, var4);
      }
   }

   @ParametersAreNonnullByDefault
   private boolean applyBoneMeal(Block var1, Block var2, BlockMenu var3) {
      for (int var8 : this.getInputSlots()) {
         if (this.isFertilizer(var3.getItemInSlot(var8))) {
            this.removeCharge(var1.getLocation(), this.energy_consumption);
            var2.applyBoneMeal(BlockFace.UP);
            var3.consumeItem(var8);
            var2.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, var2.getLocation().add(0.5, 0.5, 0.5), 4, 0.1F, 0.1F, 0.1F);
            return true;
         }
      }

      return false;
   }

   @ParametersAreNonnullByDefault
   private boolean updateSaplingData(Block var1, Block var2, BlockMenu var3, Sapling var4) {
      for (int var9 : this.getInputSlots()) {
         if (this.isFertilizer(var3.getItemInSlot(var9))) {
            this.removeCharge(var1.getLocation(), 24);
            var4.setStage(var4.getStage() + 1);
            var2.setBlockData(var4, false);
            var3.consumeItem(var9);
            var2.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, var2.getLocation().add(0.5, 0.5, 0.5), 4, 0.1F, 0.1F, 0.1F);
            return true;
         }
      }

      return false;
   }

   protected boolean isFertilizer(@Nullable ItemStack var1) {
      return SlimefunUtils.isItemSimilar(var1, organicFertilizer, false, false);
   }
}
