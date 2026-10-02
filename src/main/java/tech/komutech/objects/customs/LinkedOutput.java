package tech.komutech.objects.customs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public record LinkedOutput(ItemStack[] freeOutput, Map<Integer, ItemStack> linkedOutput, int[] freeChances, Map<Integer, Integer> linkedChances) {
   public ItemStack[] toArray() {
      ItemStack[] var1 = new ItemStack[this.freeOutput.length + this.linkedOutput.size()];
      System.arraycopy(this.freeOutput, 0, var1, 0, this.freeOutput.length);
      int var2 = this.freeOutput.length;

      for (Iterator var3 = this.linkedOutput.values().iterator(); var3.hasNext(); var2++) {
         var1[var2] = (ItemStack)var3.next();
      }

      return var1;
   }

   public List<Integer> chancesToArray() {
      ArrayList var1 = new ArrayList(this.freeChances.length + this.linkedChances.size());

      for (int var5 : this.freeChances) {
         var1.add(var5);
      }

      var1.addAll(this.linkedChances.values());
      return var1;
   }
}
