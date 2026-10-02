package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tech.komutech.KT;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.util.MachineOutputHelper;

public class CustomMaterialGenerator extends SlimefunItem implements InventoryBlock, EnergyNetComponent, RecipeDisplayItem {
   private final int capacity;
   private final List<Integer> output;
   private final int tickRate;
   private final int statusSlot;
   private final List<ItemStack> generation;
   private final int per;
   private final List<Integer> chances;
   private final boolean chooseOne;
   private final Random RNG;

   public CustomMaterialGenerator(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      int var5,
      List<Integer> var6,
      int var7,
      int var8,
      List<ItemStack> var9,
      CustomMenu var10,
      int var11,
      List<Integer> var12,
      boolean var13
   ) {
      super(var1, var2, var3, var4);
      this.capacity = var5;
      this.output = var6;
      this.statusSlot = var7;
      this.tickRate = var8;
      this.generation = var9;
      this.per = var11;
      this.chances = var12;
      this.chooseOne = var13;
      this.RNG = new Random();
      this.addItemHandler(new ItemHandler[]{this.getBlockTicker()});
      this.addItemHandler(new ItemHandler[]{new SimpleBlockBreakHandler() {
         public void onBlockBreak(@NotNull Block var1) {
            BlockMenu var2x = StorageCacheUtils.getMenu(var1.getLocation());
            if (var2x != null) {
               var2x.dropItems(var1.getLocation(), CustomMaterialGenerator.this.getOutputSlots());
            }
         }
      }});
      var10.addItem(var7, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
      this.createPreset(this, var2x -> {
         var10.registerInteractableSlots(this.output);
         var10.apply(var2x);
      });
      this.register(KT.plugin());
   }

   private void tick(Block var1) {
      int var2 = this.getProgress(var1);
      BlockMenu var3 = StorageCacheUtils.getMenu(var1.getLocation());
      if (var3 != null) {
         if (this.getCharge(var1.getLocation()) >= this.per) {
            if (var2 >= this.tickRate) {
               this.setProgress(var1, 0);
               this.pushItems(var3);
            } else {
               this.addProgress(var1);
            }
         } else if (var3.hasViewer() && this.statusSlot > -1) {
            var3.replaceExistingItem(this.statusSlot, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&4电力不足", new String[0]));
         }
      }
   }

   private void addProgress(Block var1) {
      this.setProgress(var1, this.getProgress(var1) + 1);
   }

   private void setProgress(Block var1, int var2) {
      StorageCacheUtils.setData(var1.getLocation(), "progress", String.valueOf(var2));
   }

   private int getProgress(Block var1) {
      int var2;
      try {
         var2 = Integer.parseInt(Objects.requireNonNull(StorageCacheUtils.getData(var1.getLocation(), "progress")));
      } catch (NumberFormatException | NullPointerException var4) {
         StorageCacheUtils.setData(var1.getLocation(), "progress", "1");
         var2 = 1;
      }

      return var2;
   }

   @NotNull
   public EnergyNetComponentType getEnergyComponentType() {
      return EnergyNetComponentType.CONSUMER;
   }

   public int getCapacity() {
      return this.capacity;
   }

   public int[] getInputSlots() {
      return new int[0];
   }

   public int[] getOutputSlots() {
      int[] var1 = new int[this.output.size()];

      for (int var2 = 0; var2 < this.output.size(); var2++) {
         var1[var2] = this.output.get(var2);
      }

      return var1;
   }

   public BlockTicker getBlockTicker() {
      return new BlockTicker() {
         public boolean isSynchronized() {
            return false;
         }

         public void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
            CustomMaterialGenerator.this.tick(var1);
         }
      };
   }

   @NotNull
   public List<ItemStack> getDisplayRecipes() {
      CustomItemStack var1 = new CustomItemStack(Material.KNOWLEDGE_BOOK, "&a&l速度", Collections.singletonList("&a&l每 &b&l" + this.tickRate + " &a&l个粘液刻生成一次"));
      ArrayList var2 = new ArrayList();

      for (ItemStack var4 : this.generation) {
         var2.add(var1);
         var2.add(var4.clone());
      }

      return var2;
   }

   private void pushItems(BlockMenu var1) {
      Block var2 = var1.getBlock();
      List<ItemStack> var3 = this.getMatchChanceResult();
      if (this.chooseOne && !var3.isEmpty()) {
         var3 = Collections.singletonList((ItemStack)var3.get(this.RNG.nextInt(var3.size())));
      }

      for (ItemStack var5 : var3) {
         if (var1.fits(var5, this.getOutputSlots())) {
            if (var1.hasViewer() && this.statusSlot > -1) {
               var1.replaceExistingItem(this.statusSlot, new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, "&a生产中", new String[0]));
            }

            MachineOutputHelper.pushOrDrop(var1, var2, var5.clone(), this.getOutputSlots());
            this.removeCharge(var2.getLocation(), this.per);
         } else if (var1.hasViewer() && this.statusSlot > -1) {
            var1.replaceExistingItem(this.statusSlot, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE, "&c空间不足", new String[0]));
         }
      }
   }

   private List<ItemStack> getMatchChanceResult() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < this.generation.size(); var2++) {
         ItemStack var3 = this.generation.get(var2);
         int var4 = this.chances.get(var2);
         if (this.matchChance(var4)) {
            var1.add(var3);
         }
      }

      return var1;
   }

   private boolean matchChance(Integer var1) {
      if (var1 == null) {
         return false;
      } else if (var1 >= 100) {
         return true;
      } else if (var1 < 1) {
         return false;
      } else {
         int var2 = this.RNG.nextInt(100);
         return var2 < var1;
      }
   }
}
