package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.AbstractEnergyProvider;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.util.MachineOutputHelper;

public class CustomGenerator extends AbstractEnergyProvider implements MachineProcessHolder<FuelOperation>, EnergyNetProvider {
   private final MachineProcessor<FuelOperation> processor = new MachineProcessor(this);
   private final int capacity;
   private final List<Integer> input;
   private final List<Integer> output;
   private final CustomMenu menu;
   private final int production;

   public CustomGenerator(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      @Nullable CustomMenu var5,
      int var6,
      List<Integer> var7,
      List<Integer> var8,
      int var9,
      List<MachineFuel> var10
   ) {
      super(var1, var2, var3, var4);
      this.processor.setProgressBar(this.getProgressBar());
      this.addItemHandler(new ItemHandler[]{this.onBlockBreak()});
      this.registerDefaultFuelTypes();
      if (var5 != null) {
         this.processor.setProgressBar(var5.getProgressBarItem());
         this.createPreset(this, var3x -> {
            var5.registerInteractableSlots(var7);
            var5.registerInteractableSlots(var8);
            var5.apply(var3x);
         });
      }

      this.capacity = var6;
      this.input = var7;
      this.output = var8;
      this.production = var9;
      this.menu = var5;

      for (MachineFuel var12 : var10) {
         this.registerFuel(var12);
      }

      this.register(KT.plugin());
   }

   public int getGeneratedOutput(@Nonnull Location var1, @Nonnull SlimefunBlockData var2) {
      BlockMenu var4 = StorageCacheUtils.getMenu(var1);
      FuelOperation var5 = (FuelOperation)this.processor.getOperation(var1);
      int var3 = this.menu != null && this.menu.getProgressSlot() != -1 ? this.menu.getProgressSlot() : 22;
      if (var4 != null) {
         if (var5 != null) {
            if (!var5.isFinished()) {
               this.processor.updateProgressBar(var4, var3, var5);
               if (this.isChargeable()) {
                  int var12 = this.getCharge(var1, var2);
                  if (this.getCapacity() - var12 >= this.getEnergyProduction()) {
                     var5.addProgress(1);
                     return this.getEnergyProduction();
                  } else {
                     return 0;
                  }
               } else {
                  var5.addProgress(1);
                  return this.getEnergyProduction();
               }
            } else {
               Block var11 = var4.getBlock();
               ItemStack var13 = var5.getIngredient();
               if (this.isBucket(var13)) {
                  MachineOutputHelper.pushOrDrop(var4, var11, new ItemStack(Material.BUCKET), this.getOutputSlots());
               }

               if (var5.getResult() != null) {
                  MachineOutputHelper.pushOrDrop(var4, var11, var5.getResult().clone(), this.getOutputSlots());
               }

               ItemStack var14 = this.menu == null ? ChestMenuUtils.getBackground() : this.menu.getItems().getOrDefault(var3, ChestMenuUtils.getBackground());
               var4.replaceExistingItem(var3, var14);
               this.processor.endOperation(var1);
               return 0;
            }
         } else {
            HashMap<Integer, Integer> var7 = new HashMap<>();
            MachineFuel var8 = this.findRecipe(var4, var7);
            if (var8 != null) {
               for (Entry var10 : var7.entrySet()) {
                  var4.consumeItem((Integer)var10.getKey(), (Integer)var10.getValue());
               }

               this.processor.startOperation(var1, new FuelOperation(var8));
            }

            return 0;
         }
      } else {
         return 0;
      }
   }

   private boolean isBucket(ItemStack var1) {
      if (var1 == null) {
         return false;
      } else {
         ItemStackWrapper var2 = ItemStackWrapper.wrap(var1);
         return var1.getType() == Material.LAVA_BUCKET
            || var1.getType() == Material.WATER_BUCKET
            || SlimefunUtils.isItemSimilar(var2, SlimefunItems.FUEL_BUCKET, true)
            || SlimefunUtils.isItemSimilar(var2, SlimefunItems.OIL_BUCKET, true);
      }
   }

   private MachineFuel findRecipe(BlockMenu var1, Map<Integer, Integer> var2) {
      for (MachineFuel var4 : this.fuelTypes) {
         for (int var8 : this.getInputSlots()) {
            if (var4.test(var1.getItemInSlot(var8))) {
               var2.put(var8, var4.getInput().getAmount());
               return var4;
            }
         }
      }

      return null;
   }

   @Nonnull
   protected BlockBreakHandler onBlockBreak() {
      return new SimpleBlockBreakHandler() {
         public void onBlockBreak(@NotNull Block var1) {
            BlockMenu var2 = StorageCacheUtils.getMenu(var1.getLocation());
            if (var2 != null) {
               var2.dropItems(var1.getLocation(), CustomGenerator.this.getInputSlots());
               var2.dropItems(var1.getLocation(), CustomGenerator.this.getOutputSlots());
            }

            CustomGenerator.this.processor.endOperation(var1);
         }
      };
   }

   @NotNull
   public String getInventoryTitle() {
      return "";
   }

   @NotNull
   public ItemStack getProgressBar() {
      return new ItemStack(Material.FLINT_AND_STEEL);
   }

   public int getEnergyProduction() {
      return this.production;
   }

   @NotNull
   public MachineProcessor<FuelOperation> getMachineProcessor() {
      return this.processor;
   }

   public int getCapacity() {
      return this.capacity;
   }

   public int[] getInputSlots() {
      if (this.input == null) {
         return new int[0];
      } else {
         int[] var1 = new int[this.input.size()];

         for (int var2 = 0; var2 < this.input.size(); var2++) {
            var1[var2] = this.input.get(var2);
         }

         return var1;
      }
   }

   public int[] getOutputSlots() {
      if (this.output == null) {
         return new int[0];
      } else {
         int[] var1 = new int[this.output.size()];

         for (int var2 = 0; var2 < this.output.size(); var2++) {
            var1[var2] = this.output.get(var2);
         }

         return var1;
      }
   }

   public void registerDefaultFuelTypes() {
   }

   public MachineProcessor<FuelOperation> getProcessor() {
      return this.processor;
   }
}
