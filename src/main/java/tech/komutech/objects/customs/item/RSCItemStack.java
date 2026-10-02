package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class RSCItemStack extends CustomItemStack {
   public RSCItemStack(ItemStack var1, String var2, List<String> var3) {
      super(var1, var2x -> {
         if (var2 != null && !var2.isBlank()) {
            var2x.setDisplayName(var2);
         }

         if (var3 != null && !var3.isEmpty()) {
            var2x.setLore(var3);
         }
      });
   }
}
