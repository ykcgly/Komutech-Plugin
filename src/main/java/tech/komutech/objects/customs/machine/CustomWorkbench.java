package tech.komutech.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.objects.customs.CustomMenu;
import tech.komutech.objects.machine.CustomLinkedMachineRecipe;
import tech.komutech.script.ScriptEval;
import tech.komutech.util.BlockMenuUtil;
import tech.komutech.util.ExceptionHandler;
import tech.komutech.util.StackUtils;

public class CustomWorkbench extends AContainer implements EnergyNetComponent, RecipeDisplayItem {
   private final BlockTicker blockTicker = new BlockTicker() {
      public boolean isSynchronized() {
         return false;
      }

      public void tick(Block var1, SlimefunItem var2, SlimefunBlockData var3) {
      }
   };
   private final MachineProcessor<CraftingOperation> processor = new MachineProcessor(this);
   private final int[] input;
   private final int[] output;
   private final List<CustomLinkedMachineRecipe> raw_recipes;
   private final List<CustomLinkedMachineRecipe> recipes;
   private final int energyPerCraft;
   private final int capacity;
   private final boolean hideAllRecipes;
   private final int click;
   private final ScriptEval eval;
   public static final ItemStack RECIPE_INPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输入", new String[]{"", "&2> &a点击查看"});
   public static final ItemStack RECIPE_OUTPUT = new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, "&a多物品输出", new String[]{"", "&2> &a点击查看"});
   @Nullable
   private final CustomMenu menu;

   public CustomWorkbench(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      final int[] var5,
      final int[] var6,
      List<CustomLinkedMachineRecipe> var7,
      int var8,
      int var9,
      @Nullable final CustomMenu var10,
      boolean var11,
      int var12,
      @Nullable ScriptEval var13
   ) {
      super(var1, var2, var3, var4);
      this.input = var5;
      this.output = var6;
      this.raw_recipes = var7;
      this.recipes = new ArrayList<>(this.raw_recipes.stream().filter(var0 -> !var0.isForDisplay()).toList());
      this.energyPerCraft = var8;
      this.capacity = var9;
      this.menu = var10;
      this.hideAllRecipes = var11;
      this.click = var12;
      this.eval = var13;
      if (var10 == null) {
         ExceptionHandler.handleError("未找到菜单 " + var2.getItemId());
      } else {
         this.processor.setProgressBar(var10.getProgressBarItem());
         if (var13 != null) {
            var13.doInit();
         }

         new BlockMenuPreset(this.getId(), this.getItemName()) {
            public void init() {
               var10.registerInteractableSlots(CustomWorkbench.this.input);
               var10.registerInteractableSlots(CustomWorkbench.this.output);
               var10.apply(this);
            }

            public void newInstance(@NotNull BlockMenu var1, @NotNull Block var2x) {
               var1.addMenuClickHandler(CustomWorkbench.this.click, (var2xx, var3x, var4x, var5xx) -> {
                  if (CustomWorkbench.this.eval != null) {
                     return CustomWorkbench.this.eval.evalFunction("onClick", this, var2xx, var3x, var4x, var5xx) instanceof Boolean var7x ? var7x : false;
                  } else if (!CustomWorkbench.this.takeCharge(var1.getLocation())) {
                     return false;
                  } else {
                     CustomLinkedMachineRecipe var6x = CustomWorkbench.this.findNextLinkedRecipe(var1);
                     if (var6x != null) {
                        BlockMenuUtil.pushItem(var1, var6x.getLinkedOutput(), var6x.isChooseOneIfHas());
                     }

                     return false;
                  }
               });
            }

            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow var1) {
               return var1 == ItemTransportFlow.INSERT ? var5 : var6;
            }

            public boolean canOpen(Block var1, Player var2x) {
               return var2x.hasPermission("slimefun.inventory.bypass")
                  ? true
                  : CustomWorkbench.this.canUse(var2x, false)
                     && Slimefun.getProtectionManager().hasPermission(var2x, var1.getLocation(), Interaction.INTERACT_BLOCK);
            }
         };
         this.setProcessingSpeed(1);
         this.setCapacity(var9);
         this.setEnergyConsumption(var8);
         this.register(KT.plugin());
      }
   }

   @NotNull
   protected BlockBreakHandler onBlockBreak() {
      return new SimpleBlockBreakHandler() {
         public void onBlockBreak(@NotNull Block var1) {
            BlockMenu var2 = StorageCacheUtils.getMenu(var1.getLocation());
            if (var2 != null) {
               var2.dropItems(var1.getLocation(), CustomWorkbench.this.getInputSlots());
               var2.dropItems(var1.getLocation(), CustomWorkbench.this.getOutputSlots());
            }

            CustomWorkbench.this.processor.endOperation(var1);
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
                  ItemStack var7 = SingleItemRecipeGuideListener.tagItemWorkbenchRecipe(RECIPE_INPUT, var2);
                  var1.add(var7);
               }

               if (var6.length == 1) {
                  var1.add(var6[0]);
               } else {
                  ItemStack var8 = SingleItemRecipeGuideListener.tagItemWorkbenchRecipe(RECIPE_OUTPUT, var2);
                  var1.add(var8);
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

   @NotNull
   public EnergyNetComponentType getEnergyComponentType() {
      return EnergyNetComponentType.CONSUMER;
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
   }

   @Nullable
   public CustomLinkedMachineRecipe findNextLinkedRecipe(BlockMenu var1) {
      for (CustomLinkedMachineRecipe var3 : this.recipes) {
         Map<Integer, ItemStack> var4 = var3.getLinkedInput();
         boolean var5 = true;

         for (int var7 : var4.keySet()) {
            ItemStack var8 = var1.getItemInSlot(var7);
            if (!StackUtils.itemsMatch(var8, (ItemStack)var4.get(var7), false, true)) {
               var5 = false;
               break;
            }
         }

         if (var5 && BlockMenuUtil.fits(var1, var3.getLinkedOutput())) {
            for (int var10 : var4.keySet()) {
               ItemStack var11;
               if (!var3.getNoConsumes().contains(var10) && (var11 = var1.getItemInSlot(var10)) != null && var11.getType() != Material.AIR) {
                  var1.consumeItem(var10, ((ItemStack)var4.get(var10)).getAmount());
               }
            }

            return var3;
         }
      }

      return null;
   }

   public BlockTicker getBlockTicker() {
      return this.blockTicker;
   }

   @Nullable
   public CustomMenu getMenu() {
      return this.menu;
   }
}
