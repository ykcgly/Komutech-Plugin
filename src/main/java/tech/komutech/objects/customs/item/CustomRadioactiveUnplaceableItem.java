package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import org.bukkit.inventory.ItemStack;
import tech.komutech.script.ScriptEval;

public final class CustomRadioactiveUnplaceableItem extends CustomUnplaceableItem implements Radioactive {
   private final Radioactivity radioactivity;
   private final Object[] constructorArgs;

   public CustomRadioactiveUnplaceableItem(
      ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, ScriptEval var5, ItemStack var6, Radioactivity var7
   ) {
      super(var1, var2, var3, var4, var5, var6);
      this.radioactivity = var7;
      this.constructorArgs = new Object[]{var1, var2, var3, var4, var5, var6, var7};
   }

   public Radioactivity getRadioactivity() {
      return this.radioactivity;
   }

   @Override
   public Object[] constructorArgs() {
      return this.constructorArgs;
   }
}
