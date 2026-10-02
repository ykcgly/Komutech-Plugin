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
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.machine.CustomLinkedMachineOperation;
import tech.komutech.objects.machine.CustomLinkedMachineRecipe;
import tech.komutech.util.BlockMenuUtil;
import tech.komutech.util.CommonUtils;
import tech.komutech.util.ExceptionHandler;
import tech.komutech.util.StackUtils;

public class CustomLinkedRecipeMachine extends AContainer implements RecipeDisplayItem {
   private static final Map<Location, Integer> lastMatch = new HashMap<>();
   private final MachineProcessor<CraftingOperation> processor = new MachineProcessor(this);
   private final int[] input;
   private final int[] output;
   private final List<CustomLinkedMachineRecipe> raw_recipes;
   private final List<CustomLinkedMachineRecipe> recipes;
   private final int energyPerCraft;
   private final int capacity;
   private final boolean hideAllRecipes;
   private final int saveAmount;
   public static final ItemStack RECIPE_INPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输入", new String[]{"", "&2> &a点击查看"});
   public static final ItemStack RECIPE_OUTPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输出", new String[]{"", "&2> &a点击查看"});
   @Nullable
   private final CustomMenu menu;

   public CustomLinkedRecipeMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      int[] var5,
      int[] var6,
      List<CustomLinkedMachineRecipe> var7,
      int var8,
      int var9,
      @Nullable CustomMenu var10,
      int var11,
      boolean var12,
      int var13
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
      this.saveAmount = var13;
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
               var2.dropItems(var1.getLocation(), CustomLinkedRecipeMachine.this.getInputSlots());
               var2.dropItems(var1.getLocation(), CustomLinkedRecipeMachine.this.getOutputSlots());
            }

            CustomLinkedRecipeMachine.this.processor.endOperation(var1);
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

         for (CustomLinkedMachineRecipe var4 : this.raw_recipes) {
            if (!var4.isHide()) {
               ItemStack[] var5 = var4.getInput();
               ItemStack[] var6 = var4.getOutput();
               if (var5.length == 1) {
                  var1.add(var5[0]);
               } else {
                  ItemStack var7 = SingleItemRecipeGuideListener.tagItemLinkedRecipe(RECIPE_INPUT, var2);
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
                  ItemStack var11 = SingleItemRecipeGuideListener.tagItemLinkedRecipe(RECIPE_OUTPUT, var2);
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
      CustomLinkedMachineOperation var4 = (CustomLinkedMachineOperation)this.processor.getOperation(var1);
      int var2 = this.menu != null && this.menu.getProgressSlot() != -1 ? this.menu.getProgressSlot() : 22;
      if (var3 != null) {
         if (var4 != null) {
            if (this.takeCharge(var1.getLocation())) {
               if (!var4.isFinished()) {
                  if (var3.hasViewer()) {
                     this.processor.updateProgressBar(var3, var2, var4);
                  }

                  var4.addProgress(1);
               } else {
                  CustomLinkedMachineRecipe var6 = var4.getRecipe();
                  if (var6 != null) {
                     BlockMenuUtil.pushItem(var3, var6.getLinkedOutput(), var6.isChooseOneIfHas());
                  }

                  ItemStack var7 = this.menu == null ? ChestMenuUtils.getBackground() : this.menu.getItems().getOrDefault(var2, ChestMenuUtils.getBackground());
                  var3.replaceExistingItem(var2, var7);
                  this.processor.endOperation(var1);
               }
            }
         } else {
            CustomLinkedMachineRecipe var9 = this.findNextLinkedRecipe(var3);
            if (var9 == null) {
               return;
            }

            var4 = new CustomLinkedMachineOperation(var9);
            this.processor.startOperation(var1, var4);
            if (var3.hasViewer()) {
               this.processor.updateProgressBar(var3, var2, var4);
            }
         }
      }
   }

   @Nullable
   public CustomLinkedMachineRecipe findNextLinkedRecipe(BlockMenu var1) {
      Location var2 = var1.getLocation();
      Integer var3 = lastMatch.get(var1.getLocation());
      if (var3 != null) {
         CustomLinkedMachineRecipe var4 = this.recipes.get(var3);
         if (this.matchRecipe(var1, var4)) {
            return var4;
         }

         lastMatch.remove(var2);
      }

      for (int var6 = 0; var6 < this.recipes.size(); var6++) {
         CustomLinkedMachineRecipe var5;
         if ((var3 == null || var6 != var3) && this.matchRecipe(var1, var5 = this.recipes.get(var6))) {
            lastMatch.put(var2, var6);
            return var5;
         }
      }

      return null;
   }

   private boolean matchRecipe(BlockMenu var1, CustomLinkedMachineRecipe var2) {
      Map<Integer, ItemStack> var3 = var2.getLinkedInput();
      boolean var4 = true;
      if (!BlockMenuUtil.fits(var1, var2.getLinkedOutput())) {
         return false;
      } else {
         for (int var6 : var3.keySet()) {
            ItemStack var7 = var1.getItemInSlot(var6);
            if (this.saveAmount > 0) {
               if (var7 != null) {
                  ItemStack var8;
                  if (var7.getMaxStackSize() == 1) {
                     var8 = var7.clone();
                  } else {
                     if (var7.getAmount() <= this.saveAmount) {
                        var4 = false;
                        break;
                     }

                     var8 = var7.clone();
                     var8.setAmount(var8.getAmount() - this.saveAmount);
                  }

                  if (!StackUtils.itemsMatch(var8, (ItemStack)var3.get(var6), false, true)) {
                     var4 = false;
                     break;
                  }
               } else if (var3.get(var6) != null) {
                  var4 = false;
                  break;
               }
            } else if (!StackUtils.itemsMatch(var7, (ItemStack)var3.get(var6), false, true)) {
               var4 = false;
               break;
            }
         }

         if (!var4) {
            return false;
         } else {
            for (int var10 : var3.keySet()) {
               if (!var2.getNoConsumes().contains(var10)) {
                  var1.consumeItem(var10, ((ItemStack)var3.get(var10)).getAmount());
               }
            }

            return true;
         }
      }
   }

   @Nullable
   public CustomMenu getMenu() {
      return this.menu;
   }
}
