package tech.komutech.objects.machine;

import it.unimi.dsi.fastutil.ints.IntList;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.inventory.ItemStack;
import tech.komutech.util.FastItemMatch;

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

   /**
    * 输入堆的预解析匹配规格（懒加载缓存）：getInput() 在注册后不再变化，
    * 每个 Spec 只需解析一次，供模板机器每 tick 的输入比对复用——
    * 避免每次比较都走 SlimefunUtils.isItemSimilar 的全注册表扫描。
    */
   public FastItemMatch.Spec[] inputSpecs() {
      FastItemMatch.Spec[] s = this.inputSpecs;
      if (s == null) {
         ItemStack[] in = this.getInput();
         s = new FastItemMatch.Spec[in.length];
         for (int i = 0; i < in.length; i++) {
            s[i] = FastItemMatch.Spec.of(in[i]);
         }

         this.inputSpecs = s;
      }

      return s;
   }

   private volatile FastItemMatch.Spec[] inputSpecs;
}
