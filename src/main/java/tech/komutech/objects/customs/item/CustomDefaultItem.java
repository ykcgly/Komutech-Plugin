package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;
import tech.komutech.objects.customs.parent.CustomItem;

public class CustomDefaultItem extends CustomItem {
   private final Object[] constructorArgs;

   public CustomDefaultItem(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, ItemStack var5) {
      super(var1, var2, var3, var4, var5);
      this.constructorArgs = new Object[]{var1, var2, var3, var4, var5};
   }

   @Override
   public Object[] constructorArgs() {
      return this.constructorArgs;
   }
}
