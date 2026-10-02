package tech.komutech.objects.customs.parent;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

public abstract class CustomItem extends SlimefunItem {
   public CustomItem(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, ItemStack var5) {
      super(var1, var2, var3, var4, var5);
   }

   public abstract Object[] constructorArgs();
}
