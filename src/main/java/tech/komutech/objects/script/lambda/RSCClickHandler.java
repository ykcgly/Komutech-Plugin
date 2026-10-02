package tech.komutech.objects.script.lambda;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface RSCClickHandler extends MenuClickHandler {
   void mainFunction(Player var1, int var2, ItemStack var3, ClickAction var4);

   default boolean onClick(Player var1, int var2, ItemStack var3, ClickAction var4) {
      this.mainFunction(var1, var2, var3, var4);
      this.andThen(var1, var2, var3, var4);
      return false;
   }

   default void andThen(Player var1, int var2, ItemStack var3, ClickAction var4) {
   }
}
