package tech.komutech.objects.customs.item.exts;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.RainbowTickHandler;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tech.komutech.objects.customs.parent.CustomItem;

public class CustomRainbowBlock extends CustomItem {
   private final Object[] constructorArgs;

   public CustomRainbowBlock(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, RainbowTickHandler var5, ItemStack var6) {
      super(var1, var2, var3, var4, var6);
      this.addItemHandler(new ItemHandler[]{var5});
      this.constructorArgs = new Object[]{var1, var2, var3, var4, var5, var6};
   }

   public CustomRainbowBlock(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, ColoredMaterial var5, ItemStack var6) {
      this(var1, var2, var3, var4, new RainbowTickHandler(var5), var6);
   }

   public CustomRainbowBlock(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, List<Material> var5, ItemStack var6) {
      this(var1, var2, var3, var4, new RainbowTickHandler(var5), var6);
   }

   @Override
   public Object[] constructorArgs() {
      return this.constructorArgs;
   }
}
