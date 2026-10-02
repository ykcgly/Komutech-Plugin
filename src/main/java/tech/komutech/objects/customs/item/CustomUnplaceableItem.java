package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tech.komutech.objects.customs.parent.CustomItem;
import tech.komutech.script.ScriptEval;

public class CustomUnplaceableItem extends CustomItem implements NotPlaceable {
   private final Object[] constructorArgs;

   public CustomUnplaceableItem(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, @Nullable ScriptEval var5, ItemStack var6) {
      super(var1, var2, var3, var4, var6);
      if (var5 != null) {
         var5.doInit();
         this.addItemHandler(
            new ItemHandler[]{
               (ItemUseHandler)var1x -> {
                  var5.evalFunction("onUse", var1x);
                  var1x.cancel();
               },
               (WeaponUseHandler)(var1x, var2x, var3x) -> var5.evalFunction("onWeaponHit", var1x, var2x, var3x),
               (ToolUseHandler)(var1x, var2x, var3x, var4x) -> var5.evalFunction("onToolUse", var1x, var2x, var3x, var4x)
            }
         );
      } else {
         this.addItemHandler(new ItemHandler[]{(io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler)PlayerRightClickEvent::cancel});
      }

      this.constructorArgs = new Object[]{var1, var2, var3, var4, var5, var6};
   }

   @Override
   public Object[] constructorArgs() {
      return this.constructorArgs;
   }
}
