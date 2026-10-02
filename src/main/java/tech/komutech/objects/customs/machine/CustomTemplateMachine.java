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
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tech.komutech.KT;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.customs.parent.AbstractEmptyMachine;
import tech.komutech.objects.machine.CustomMachineRecipe;
import tech.komutech.objects.machine.CustomTemplateCraftingOperation;
import tech.komutech.objects.machine.MachineTemplate;
import tech.komutech.util.CommonUtils;
import tech.komutech.util.ItemUtils;
import tech.komutech.util.MachineOutputHelper;

public class CustomTemplateMachine extends AbstractEmptyMachine<CustomTemplateCraftingOperation> implements RecipeDisplayItem, EnergyNetComponent {
   private final MachineProcessor<CustomTemplateCraftingOperation> processor = new MachineProcessor(this);
   private final CustomMenu menu;
   private final List<Integer> inputSlots;
   private final List<Integer> outputSlots;
   private final int templateSlot;
   private final List<MachineTemplate> templates;
   private final int consumption;
   private final int capacity;
   private final boolean fasterIfMoreTemplates;
   private final boolean moreOutputIfMoreTemplates;
   private final boolean hideAllRecipes;
   private final int[] inputSlotArray;
   private final int[] outputSlotArray;

   public CustomTemplateMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      @NotNull CustomMenu var5,
      List<Integer> var6,
      List<Integer> var7,
      final int var8,
      List<MachineTemplate> var9,
      int var10,
      int var11,
      boolean var12,
      boolean var13,
      boolean var14
   ) {
      super(var1, var2, var3, var4);
      this.menu = var5;
      this.inputSlots = var6;
      this.outputSlots = var7;
      this.templateSlot = var8;
      this.templates = var9;
      this.consumption = var10;
      this.capacity = var11;
      this.fasterIfMoreTemplates = var12;
      this.moreOutputIfMoreTemplates = var13;
      this.hideAllRecipes = var14;
      this.inputSlotArray = var6.stream().mapToInt(Integer::intValue).toArray();
      this.outputSlotArray = var7.stream().mapToInt(Integer::intValue).toArray();
      this.createPreset(this, var2x -> {
         var5.registerInteractableSlots(this.inputSlotArray);
         var5.registerInteractableSlots(this.outputSlotArray);
         var5.registerInteractableSlots(this.templateSlot);
         var5.apply(var2x);
      });
      this.addItemHandler(new ItemHandler[]{this.getBlockTicker()});
      this.addItemHandler(new ItemHandler[]{new SimpleBlockBreakHandler() {
         public void onBlockBreak(@NotNull Block var1) {
            BlockMenu var2x = StorageCacheUtils.getMenu(var1.getLocation());
            if (var2x != null) {
               var2x.dropItems(var1.getLocation(), new int[]{var8});
               var2x.dropItems(var1.getLocation(), CustomTemplateMachine.this.getOutputSlots());
               var2x.dropItems(var1.getLocation(), CustomTemplateMachine.this.getInputSlots());
            }
         }
      }});
      this.processor.setProgressBar(var5.getProgressBarItem());
      this.register(KT.plugin());
   }

   @Override
   public BlockTicker getBlockTicker() {
      return new BlockTicker() {
         public boolean isSynchronized() {
            return true;
         }

         public void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
            CustomTemplateMachine.this.tick(var1, var2, var3);
         }
      };
   }

   @NotNull
   public List<ItemStack> getDisplayRecipes() {
      ArrayList var1 = new ArrayList();
      if (this.hideAllRecipes) {
         return var1;
      } else {
         int var2 = 0;
         int var3 = 0;

         for (MachineTemplate var5 : this.templates) {
            for (CustomMachineRecipe var7 : var5.recipes()) {
               if (!var7.isHide()) {
                  if (var7.getInput().length == 0) {
                     ItemStack var8 = var5.template().clone();
                     CommonUtils.addLore(var8, true, "&d&l&o*模板物品不消耗*");
                     var1.add(var8);
                  } else {
                     var1.add(SingleItemRecipeGuideListener.tagItemTemplateRecipe(CustomRecipeMachine.RECIPE_INPUT, var2, var3));
                  }

                  if (var7.getOutput().length == 1) {
                     int var11 = var7.getTicks() / 2;
                     ItemStack var9 = var7.getOutput()[0].clone();
                     String var10 = "&e制作时间: &b" + var11 + "&es";
                     if (var11 > 60) {
                        var10 = var10.concat("(" + CommonUtils.formatSeconds(var11) + "&e)");
                     }

                     CommonUtils.addLore(var9, true, var10);
                     var1.add(var9);
                  } else {
                     var1.add(SingleItemRecipeGuideListener.tagItemTemplateRecipe(CustomRecipeMachine.RECIPE_OUTPUT, var2, var3));
                  }

                  var3++;
               }
            }

            var2++;
            var3 = 0;
         }

         return var1;
      }
   }

   private void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
      BlockMenu var4 = var3.getBlockMenu();
      if (var4 != null) {
         ItemStack var5 = var4.getItemInSlot(this.templateSlot);
         if (var5 == null || var5.getType() == Material.AIR) {
            if (this.menu.getProgressSlot() >= 0) {
               var4.replaceExistingItem(
                  this.menu.getProgressSlot(), this.menu.getItems().getOrDefault(this.menu.getProgressSlot(), ChestMenuUtils.getBackground())
               );
            }

            this.processor.endOperation(var1);
            return;
         }

         CustomTemplateCraftingOperation var6 = (CustomTemplateCraftingOperation)this.processor.getOperation(var1);
         if (var6 != null) {
            if (!var6.getTemplate().isItemSimilar(var5)) {
               this.processor.endOperation(var1);
               if (this.menu.getProgressSlot() >= 0) {
                  var4.replaceExistingItem(
                     this.menu.getProgressSlot(), this.menu.getItems().getOrDefault(this.menu.getProgressSlot(), ChestMenuUtils.getBackground())
                  );
               }

               return;
            }

            if (this.takeCharge(var1.getLocation())) {
               if (!var6.isFinished()) {
                  this.processor.updateProgressBar(var4, this.menu.getProgressSlot(), var6);
                  var6.addProgress(1);
               } else {
                  if (this.menu.getProgressSlot() >= 0) {
                     var4.replaceExistingItem(
                        this.menu.getProgressSlot(), this.menu.getItems().getOrDefault(this.menu.getProgressSlot(), ChestMenuUtils.getBackground())
                     );
                  }

                  List<ItemStack> var7 = var6.getRecipe().getMatchChanceResult();
                  CustomMachineRecipe var8 = var6.getRecipe();
                  if (var8.isChooseOneIfHas() && !var7.isEmpty()) {
                     ItemStack var14 = (ItemStack)var7.get(ThreadLocalRandom.current().nextInt(var7.size()));
                     if (var14 != null && !var14.getType().isAir()) {
                        ItemStack var16 = var14.clone();
                        if (this.moreOutputIfMoreTemplates) {
                           var16.setAmount(var16.getAmount() * var5.getAmount());
                        }

                        MachineOutputHelper.pushOrDrop(var4, var1, var16, this.getOutputSlots());
                     }
                  } else {
                     for (ItemStack var10 : var7) {
                        if (var10 != null && !var10.getType().isAir()) {
                           ItemStack var11 = var10.clone();
                           if (this.moreOutputIfMoreTemplates) {
                              var11.setAmount(var11.getAmount() * var5.getAmount());
                           }

                           MachineOutputHelper.pushOrDrop(var4, var1, var11, this.getOutputSlots());
                        }
                     }
                  }

                  this.processor.endOperation(var1);
               }
            }
         } else {
            for (MachineTemplate var13 : this.templates) {
               if (var13.isItemSimilar(var5)) {
                  CustomMachineRecipe var15 = this.findNextRecipe(var13, var4);
                  if (var15 != null) {
                     int var17 = var15.getTicks();
                     if (this.fasterIfMoreTemplates && var5.getAmount() > 1) {
                        var17 /= var5.getAmount();
                     }

                     this.processor.startOperation(var1, new CustomTemplateCraftingOperation(var13, var15, var17));
                     break;
                  }
               }
            }
         }
      }
   }

   private boolean takeCharge(Location var1) {
      Validate.notNull(var1, "Can't attempt to take charge from a null location!");
      if (this.isChargeable()) {
         int var2 = this.getCharge(var1);
         if (var2 < this.consumption) {
            return false;
         } else {
            this.setCharge(var1, var2 - this.consumption);
            return true;
         }
      } else {
         return true;
      }
   }

   @NotNull
   public MachineProcessor<CustomTemplateCraftingOperation> getMachineProcessor() {
      return this.processor;
   }

   public int[] getInputSlots() {
      return this.inputSlotArray;
   }

   public int[] getOutputSlots() {
      return this.outputSlotArray;
   }

   private CustomMachineRecipe findNextRecipe(MachineTemplate var1, BlockMenu var2) {
      List<CustomMachineRecipe> var3 = var1.recipes();
      HashMap<Integer, ItemStackWrapper> var4 = new HashMap<>();

      for (int var8 : this.inputSlotArray) {
         ItemStack var9 = var2.getItemInSlot(var8);
         if (var9 != null && !var9.getType().isAir()) {
            var4.put(var8, ItemStackWrapper.wrap(var9));
         }
      }

      HashMap<CustomMachineRecipe, LinkedHashMap<Integer, Integer>> var18 = new HashMap<>();

      for (CustomMachineRecipe var21 : var3) {
         LinkedHashMap var23 = new LinkedHashMap();
         if (var21.getInput().length != 0) {
            for (ItemStack var12 : var21.getInput()) {
               for (int var16 : this.inputSlotArray) {
                  if (!var23.containsKey(var16)) {
                     ItemStackWrapper var17 = (ItemStackWrapper)var4.get(var16);
                     if (var17 != null && SlimefunUtils.isItemSimilar(var17, var12, true)) {
                        var23.put(var16, var12.getAmount());
                        break;
                     }
                  }
               }

               if (var23.size() == var21.getInput().length) {
                  var18.put(var21, var23);
               }
            }
         } else if (this.canFitRecipeOutput(var2, var21)) {
            if (this.inputSlotArray.length == 0) {
               return var21;
            }

            boolean var27 = true;

            for (int var38 : this.inputSlotArray) {
               if (var38 != this.templateSlot) {
                  ItemStack var39 = var2.getItemInSlot(var38);
                  if (var39 != null && !var39.getType().isAir()) {
                     var27 = false;
                     break;
                  }
               }
            }

            if (var27) {
               return var21;
            }
         }
      }

      if (var18.isEmpty()) {
         return null;
      } else {
         Entry<CustomMachineRecipe, LinkedHashMap<Integer, Integer>> var20 = null;
         int var22 = 0;

         for (Entry<CustomMachineRecipe, LinkedHashMap<Integer, Integer>> var28 : var18.entrySet()) {
            ItemStack[] var31 = var28.getKey().getInput();
            int var34 = ItemUtils.getAllItemTypeAmount(var31) * 1000 + ItemUtils.getAllItemAmount(var31);
            if (var34 > var22) {
               var20 = var28;
               var22 = var34;
            }
         }

         if (var20 == null) {
            return null;
         } else {
            CustomMachineRecipe var25 = var20.getKey();
            if (!this.canFitRecipeOutput(var2, var25)) {
               return null;
            } else {
               IntList var29 = var25.getNoConsume();
               int var32 = 0;

               for (Entry<Integer, Integer> var37 : var20.getValue().entrySet()) {
                  if (!var29.contains(var32)) {
                     var2.consumeItem((Integer)var37.getKey(), (Integer)var37.getValue());
                  }

                  var32++;
               }

               return var25;
            }
         }
      }
   }

   private boolean canFitRecipeOutput(BlockMenu var1, CustomMachineRecipe var2) {
      if (var2.isChooseOneIfHas()) {
         for (ItemStack var6 : var2.getOutput()) {
            if (var6 != null && !var6.getType().isAir() && var1.fits(var6, this.outputSlotArray)) {
               return true;
            }
         }

         return false;
      } else {
         return InvUtils.fitAll(var1.toInventory(), var2.getOutput(), this.outputSlotArray);
      }
   }

   @NotNull
   public EnergyNetComponentType getEnergyComponentType() {
      return EnergyNetComponentType.CONSUMER;
   }

   public int getCapacity() {
      return this.capacity;
   }

   public CustomMenu getMenu() {
      return this.menu;
   }

   public int getTemplateSlot() {
      return this.templateSlot;
   }

   public List<MachineTemplate> getTemplates() {
      return this.templates;
   }
}
