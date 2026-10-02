package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import it.unimi.dsi.fastutil.ints.IntList;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.machine.CustomCraftingOperation;
import tech.komutech.objects.machine.CustomMachineRecipe;
import tech.komutech.util.CommonUtils;
import tech.komutech.util.ExceptionHandler;
import tech.komutech.util.MachineOutputHelper;

public class CustomRecipeMachine extends AContainer implements RecipeDisplayItem {
   private final MachineProcessor<CraftingOperation> processor = new MachineProcessor(this);
   private final int[] input;
   private final int[] output;
   private final List<CustomMachineRecipe> raw_recipes;
   private final List<CustomMachineRecipe> recipes;
   private final int energyPerCraft;
   private final int capacity;
   private final boolean hideAllRecipes;
   public static final ItemStack RECIPE_INPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输入", new String[]{"", "&2> &a点击查看"});
   public static final ItemStack RECIPE_OUTPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输出", new String[]{"", "&2> &a点击查看"});
   @Nullable
   private final CustomMenu menu;

   public CustomRecipeMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      int[] var5,
      int[] var6,
      List<CustomMachineRecipe> var7,
      int var8,
      int var9,
      @Nullable CustomMenu var10,
      int var11,
      boolean var12
   ) {
      super(var1, var2, var3, var4);
      this.input = var5;
      this.output = var6;
      this.raw_recipes = var7;
      this.recipes = new ArrayList<>(this.raw_recipes.stream().filter(var0 -> !var0.isForDisplay()).toList());
      this.energyPerCraft = var8;
      this.capacity = var9;
      this.menu = var10;
      this.hideAllRecipes = var12;
      if (var10 == null) {
         ExceptionHandler.handleWarning("未找到菜单 " + var2.getItemId() + " 使用默认菜单");
         this.createPreset(this, this.getInventoryTitle(), var1x -> super.constructMenu(var1x));
      }

      if (var10 != null) {
         this.processor.setProgressBar(var10.getProgressBarItem());
         this.createPreset(this, var2x -> {
            var10.registerInteractableSlots(this.input);
            var10.registerInteractableSlots(this.output);
            var10.apply(var2x);
         });
      }

      this.setProcessingSpeed(var11);
      this.setCapacity(var9);
      this.setEnergyConsumption(var8);
      this.register(KT.plugin());
   }

   @NotNull
   protected BlockBreakHandler onBlockBreak() {
      return new SimpleBlockBreakHandler() {
         public void onBlockBreak(@NotNull Block var1) {
            BlockMenu var2 = StorageCacheUtils.getMenu(var1.getLocation());
            if (var2 != null) {
               var2.dropItems(var1.getLocation(), CustomRecipeMachine.this.getInputSlots());
               var2.dropItems(var1.getLocation(), CustomRecipeMachine.this.getOutputSlots());
            }

            CustomRecipeMachine.this.processor.endOperation(var1);
         }
      };
   }

   protected void registerDefaultRecipes() {
      if (this.recipes != null && !this.recipes.isEmpty()) {
         this.recipes.forEach(var1 -> super.registerRecipe(var1));
      }
   }

   public int getEnergyConsumption() {
      return this.energyPerCraft;
   }

   @NotNull
   public MachineProcessor<CraftingOperation> getMachineProcessor() {
      return this.processor;
   }

   public ItemStack getProgressBar() {
      return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
   }

   @NotNull
   public List<ItemStack> getDisplayRecipes() {
      ArrayList var1 = new ArrayList();
      if (this.hideAllRecipes) {
         return var1;
      } else {
         int var2 = 0;

         for (CustomMachineRecipe var4 : this.raw_recipes) {
            if (!var4.isHide()) {
               ItemStack[] var5 = var4.getInput();
               ItemStack[] var6 = var4.getOutput();
               if (var5.length == 1) {
                  var1.add(var5[0]);
               } else {
                  ItemStack var7 = SingleItemRecipeGuideListener.tagItemRecipe(RECIPE_INPUT, var2);
                  var1.add(var7);
               }

               if (var6.length == 1) {
                  int var10 = var4.getTicks() / 2;
                  ItemStack var8 = var6[0].clone();
                  String var9 = "&e制作时间: &b" + var10 + "&es";
                  if (var10 > 60) {
                     var9 = var9.concat("(" + CommonUtils.formatSeconds(var10) + "&e)");
                  }

                  CommonUtils.addLore(var8, true, var9);
                  var1.add(var8);
               } else {
                  ItemStack var11 = SingleItemRecipeGuideListener.tagItemRecipe(RECIPE_OUTPUT, var2);
                  var1.add(var11);
               }

               var2++;
            }
         }

         return var1;
      }
   }

   public int[] getInputSlots() {
      return this.input;
   }

   public int[] getOutputSlots() {
      return this.output;
   }

   public int getCapacity() {
      return this.capacity;
   }

   @NotNull
   public String getMachineIdentifier() {
      return this.getId();
   }

   protected void constructMenu(BlockMenuPreset var1) {
   }

   protected void tick(Block var1) {
      BlockMenu var3 = StorageCacheUtils.getMenu(var1.getLocation());
      CustomCraftingOperation var4 = (CustomCraftingOperation)this.processor.getOperation(var1);
      int var2 = this.menu != null && this.menu.getProgressSlot() != -1 ? this.menu.getProgressSlot() : 22;
      if (var3 != null) {
         if (var4 != null) {
            if (this.takeCharge(var1.getLocation())) {
               if (!var4.isFinished()) {
                  this.processor.updateProgressBar(var3, var2, var4);
                  var4.addProgress(1);
               } else {
                  CustomMachineRecipe var6 = var4.getRecipe();
                  if (var6 != null) {
                     ItemStack[] var9 = var6.getMatchChanceResult().toArray(ItemStack[]::new);
                     if (!var6.isChooseOneIfHas()) {
                        for (ItemStack var13 : var9) {
                           if (var13 != null && !var13.getType().isAir()) {
                              MachineOutputHelper.pushOrDrop(var3, var1, var13.clone(), this.getOutputSlots());
                           }
                        }
                     } else {
                        ItemStack var8;
                        if (var9.length > 0 && (var8 = var9[new SecureRandom().nextInt(var9.length)]) != null) {
                           MachineOutputHelper.pushOrDrop(var3, var1, var8.clone(), this.getOutputSlots());
                        }
                     }
                  }

                  ItemStack var7 = this.menu == null ? ChestMenuUtils.getBackground() : this.menu.getItems().getOrDefault(var2, ChestMenuUtils.getBackground());
                  var3.replaceExistingItem(var2, var7);
                  this.processor.endOperation(var1);
               }
            }
         } else {
            MachineRecipe var15 = this.findNextRecipe(var3);
            if (var15 != null) {
               CustomMachineRecipe var16 = (CustomMachineRecipe)var15;
               var4 = new CustomCraftingOperation(var16);
               this.processor.startOperation(var1, var4);
               this.processor.updateProgressBar(var3, var2, var4);
            }
         }
      }
   }

   protected MachineRecipe findNextRecipe(BlockMenu var1) {
      HashMap<Integer, ItemStackWrapper> var2 = new HashMap<>();

      for (int var6 : this.getInputSlots()) {
         ItemStack var7 = var1.getItemInSlot(var6);
         if (var7 != null) {
            var2.put(var6, ItemStackWrapper.wrap(var7));
         }
      }

      HashMap<Integer, Integer> var14 = new HashMap<>();

      for (CustomMachineRecipe var16 : this.recipes) {
         for (ItemStack var9 : var16.getInput()) {
            for (int var13 : this.getInputSlots()) {
               if (SlimefunUtils.isItemSimilar((ItemStack)var2.get(var13), var9, true)) {
                  var14.put(var13, var9.getAmount());
                  break;
               }
            }
         }

         if (var14.size() == var16.getInput().length) {
            if (!InvUtils.fitAll(var1.toInventory(), var16.getOutput(), this.getOutputSlots())) {
               return null;
            }

            ArrayList<Entry<Integer, Integer>> var18 = new ArrayList<>(var14.entrySet());
            IntList var20 = var16.getNoConsume();

            for (Entry var22 : var18) {
               int var23 = var18.indexOf(var22);
               if (!var20.contains(var23)) {
                  var1.consumeItem((Integer)var22.getKey(), (Integer)var22.getValue());
               }
            }

            return var16;
         }

         var14.clear();
      }

      return null;
   }

   @Nullable
   public CustomMenu getMenu() {
      return this.menu;
   }
}
