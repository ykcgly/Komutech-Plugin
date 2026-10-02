package tech.komutech.objects.customs.machine;

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockCraftEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.script.ScriptEval;

public class CustomMultiBlockMachine extends MultiBlockMachine {
   private final SoundEffect craftSound;
   private final int workIndex;
   private final ScriptEval eval;
   private final BlockFace dispenserFace;

   public CustomMultiBlockMachine(
      ItemGroup var1,
      SlimefunItemStack var2,
      ItemStack[] var3,
      Map<ItemStack[], ItemStack> var4,
      int var5,
      @Nullable SoundEffect var6,
      @Nullable ScriptEval var7
   ) {
      super(var1, var2, var3, BlockFace.SELF);
      this.workIndex = var5 - 1;
      this.craftSound = var6;
      this.eval = var7;
      this.dispenserFace = this.dispenserFaceGet();

      for (Entry var9 : var4.entrySet()) {
         this.addRecipe((ItemStack[])var9.getKey(), (ItemStack)var9.getValue());
      }

      // 无需自行注册 ItemUseHandler：MultiBlockMachine.register() 内部会
      // addItemHandler(getInteractionHandler())，由该 handler 转调本类的 onInteract。
      // 额外注册反而会让右键事件被处理两次。
      this.register(KT.plugin());
   }

   @Nonnull
   public List<ItemStack> getDisplayRecipes() {
      return new ArrayList<>();
   }

   public void onInteract(Player var1, Block var2) {
      Material var3 = super.getRecipe()[this.workIndex].getType();
      if (var2.getType().equals(var3)) {
         if (this.eval != null) {
            this.eval.evalFunction("onWork", var1, var2);
         }

         Block var4;
         BlockState var5;
         if ((var5 = PaperLib.getBlockState(var4 = var2.getRelative(this.dispenserFace), false).getState()) instanceof Dispenser) {
            Dispenser var6 = (Dispenser)var5;
            Inventory var7 = var6.getInventory();
            ItemStack[] var8 = var7.getContents();

            for (ItemStack[] var11 : RecipeType.getRecipeInputList(this)) {
               if (this.isCraftable(var7, var11)) {
                  ItemStack var12 = RecipeType.getRecipeOutputList(this, var11).clone();
                  MultiBlockCraftEvent var13 = new MultiBlockCraftEvent(var1, this, var11, var12);
                  Bukkit.getPluginManager().callEvent(var13);
                  if (!var13.isCancelled() && SlimefunUtils.canPlayerUseItem(var1, var12, true)) {
                     Inventory var14 = this.createVirtualInventory(var7);
                     Inventory var15 = this.findOutputInventory(var12, var4, var7, var14);
                     if (var15 != null) {
                        boolean var16 = false;

                        for (int var17 = 0; var17 < var11.length; var17++) {
                           ItemStack var18 = var8[var17];
                           if (var18 != null && var18.getType() != Material.AIR) {
                              ItemUtils.consumeItem(var18, var11[var17].getAmount(), true);
                           }
                        }

                        if (!var16) {
                           this.craftSound.playAt(var2);
                           var15.addItem(new ItemStack[]{var12});
                        }
                     } else {
                        Slimefun.getLocalization().sendMessage(var1, "machines.full-inventory", true);
                     }
                  }

                  return;
               }
            }

            if (var7.isEmpty()) {
               Slimefun.getLocalization().sendMessage(var1, "machines.inventory-empty", true);
            } else {
               Slimefun.getLocalization().sendMessage(var1, "machines.pattern-not-found", true);
            }
         }
      }
   }

   @Nonnull
   protected Inventory createVirtualInventory(@Nonnull Inventory var1) {
      Inventory var2 = Bukkit.createInventory(null, 9, Component.text("Fake Inventory"));

      for (int var3 = 0; var3 < var1.getContents().length; var3++) {
         ItemStack var4 = var1.getContents()[var3];
         if (var4 != null) {
            var4 = var4.clone();
            ItemUtils.consumeItem(var4, true);
         }

         var2.setItem(var3, var4);
      }

      return var2;
   }

   private boolean isCraftable(Inventory var1, ItemStack[] var2) {
      for (int var3 = 0; var3 < var1.getContents().length; var3++) {
         if (!SlimefunUtils.isItemSimilar(var1.getContents()[var3], var2[var3], true, true, false, false)
            && !SlimefunUtils.isItemSimilar(var1.getContents()[var3], var2[var3], false, true, false, false)) {
            return false;
         }
      }

      return true;
   }

   private BlockFace dispenserFaceGet() {
      int var3 = this.workIndex;
      ItemStack[] var4 = this.getRecipe();
      ItemStack var2;
      if (var3 - 3 > 0 && (var2 = var4[var3 - 3]) != null && var2.getType().equals(Material.DISPENSER)) {
         return BlockFace.UP;
      } else {
         ItemStack var5 = var4[var3 - 1];
         if (var5 != null && var5.getType().equals(Material.DISPENSER)) {
            return BlockFace.EAST;
         } else if (var3 + 1 >= 9) {
            return BlockFace.SELF;
         } else {
            ItemStack var6 = var4[var3 + 1];
            if (var6 != null && var6.getType().equals(Material.DISPENSER)) {
               return BlockFace.WEST;
            } else {
               ItemStack var1;
               return var3 + 3 < 8 && (var1 = var4[var3 + 3]) != null && var1.getType().equals(Material.DISPENSER) ? BlockFace.DOWN : BlockFace.SELF;
            }
         }
      }
   }
}
