package tech.komutech.objects.machine;

import it.unimi.dsi.fastutil.ints.IntList;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.inventory.ItemStack;

public class CustomMachineRecipe extends MachineRecipe {
   private final List<Integer> chances;
   private final IntList noConsume;
   private final boolean chooseOneIfHas;
   private final boolean forDisplay;
   private final boolean hide;

   public CustomMachineRecipe(int var1, ItemStack[] var2, ItemStack[] var3, List<Integer> var4, boolean var5, boolean var6, boolean var7, IntList var8) {
      super(var1, (ItemStack[])var2.clone(), (ItemStack[])var3.clone());
      this.chances = var4;
      this.chooseOneIfHas = var5;
      this.forDisplay = var6;
      this.hide = var7;
      this.noConsume = var8;
   }

   public List<ItemStack> getMatchChanceResult() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < this.getOutput().length; var2++) {
         ItemStack var3 = this.getOutput()[var2];
         if (var3 != null && !var3.getType().isAir()) {
            int var4 = this.chances.get(var2);
            if (this.matchChance(var4)) {
               var1.add(var3);
            }
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
         int var2 = new SecureRandom().nextInt(100);
         return var2 < var1;
      }
   }

   public List<Integer> getChances() {
      return this.chances;
   }

   public IntList getNoConsume() {
      return this.noConsume;
   }

   public boolean isChooseOneIfHas() {
      return this.chooseOneIfHas;
   }

   public boolean isForDisplay() {
      return this.forDisplay;
   }

   public boolean isHide() {
      return this.hide;
   }
}
