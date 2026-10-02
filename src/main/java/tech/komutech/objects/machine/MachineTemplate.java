package tech.komutech.objects.machine;

import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public record MachineTemplate(ItemStack template, List<CustomMachineRecipe> recipes) {
   public boolean isItemSimilar(ItemStack var1) {
      return var1 != null && !var1.getType().isAir() ? SlimefunUtils.isItemSimilar(var1, this.template, true, true, true) : false;
   }
}
