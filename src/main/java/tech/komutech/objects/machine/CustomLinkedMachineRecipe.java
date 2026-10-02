package tech.komutech.objects.machine;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Map;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import tech.komutech.objects.customs.LinkedOutput;

public class CustomLinkedMachineRecipe extends CustomMachineRecipe {
   private final Set<Integer> noConsumes;
   private final Map<Integer, ItemStack> linkedInput;
   private final LinkedOutput linkedOutput;

   public CustomLinkedMachineRecipe(int var1, Map<Integer, ItemStack> var2, LinkedOutput var3, boolean var4, boolean var5, boolean var6, Set<Integer> var7) {
      super(var1, var2.values().toArray(new ItemStack[0]), var3.toArray(), var3.chancesToArray(), var4, var5, var6, new IntArrayList());
      this.linkedInput = var2;
      this.linkedOutput = var3;
      this.noConsumes = var7;
   }

   public Set<Integer> getNoConsumes() {
      return this.noConsumes;
   }

   public Map<Integer, ItemStack> getLinkedInput() {
      return this.linkedInput;
   }

   public LinkedOutput getLinkedOutput() {
      return this.linkedOutput;
   }
}
