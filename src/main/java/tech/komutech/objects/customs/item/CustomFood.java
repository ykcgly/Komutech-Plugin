package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.script.ScriptEval;

public class CustomFood extends SimpleSlimefunItem<ItemConsumptionHandler> {
   private final ScriptEval eval;

   public CustomFood(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, @Nullable ScriptEval var5, ItemStack var6) {
      super(var1, var2, var3, var4, var6);
      this.eval = var5;
      this.register(KT.plugin());
   }

   @NotNull
   public ItemConsumptionHandler getItemHandler() {
      return (var1, var2, var3) -> {
         if (this.eval != null) {
            this.eval.evalFunction("onEat", var1, var2, var3);
         }
      };
   }
}
