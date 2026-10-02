package tech.komutech.native_scripts.item;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.combat.ScrollCombatEngine;
import tech.komutech.native_scripts.NativeScript;

public final class ScrollScript implements NativeScript {
   private final String skillId;

   public ScrollScript(String var1) {
      this.skillId = var1;
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("castScroll".equals(var1) && var2.length >= 3 && var2[0] instanceof Player var3) {
         ItemStack var7 = var2[1] instanceof ItemStack var5 ? var5 : var3.getInventory().getItemInMainHand();
         if (var2[2] instanceof Map var8) {
            ScrollCombatEngine.castScroll(var3, var7, var8, this.skillId);
         }
      }

      return null;
   }
}
