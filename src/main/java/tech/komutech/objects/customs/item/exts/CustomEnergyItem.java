package tech.komutech.objects.customs.item.exts;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tech.komutech.objects.customs.parent.CustomItem;
import tech.komutech.script.ScriptEval;

public class CustomEnergyItem extends CustomItem implements Rechargeable, NotPlaceable {
   private final float capacity;
   private final Object[] constructorArgs;

   public CustomEnergyItem(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, float var5, @Nullable ScriptEval var6, ItemStack var7) {
      super(var1, var2, var3, var4, var7);
      this.capacity = var5;
      if (var6 != null) {
         var6.doInit();
         this.addItemHandler(
            new ItemHandler[]{
               (ItemUseHandler)var2x -> {
                  var6.evalFunction("onUse", var2x, this);
                  var2x.cancel();
               },
               (WeaponUseHandler)(var1x, var2x, var3x) -> var6.evalFunction("onWeaponHit", var1x, var2x, var3x),
               (ToolUseHandler)(var1x, var2x, var3x, var4x) -> var6.evalFunction("onToolUse", var1x, var2x, var3x, var4x)
            }
         );
      } else {
         this.addItemHandler(new ItemHandler[]{(io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler)PlayerRightClickEvent::cancel});
      }

      this.constructorArgs = new Object[]{var1, var2, var3, var4, var5, var6, var7};
   }

   public float getMaxItemCharge(ItemStack var1) {
      return this.capacity;
   }

   @Override
   public Object[] constructorArgs() {
      return this.constructorArgs;
   }
}
