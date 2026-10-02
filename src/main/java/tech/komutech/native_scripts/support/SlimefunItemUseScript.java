package tech.komutech.native_scripts.support;

import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import tech.komutech.native_scripts.NativeScript;

public abstract class SlimefunItemUseScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onPlayerInteract".equals(var1)) {
         return null;
      } else if ("onUse".equals(var1) && var2.length != 0) {
         Optional var3 = UseEvents.parse(var2[0]);
         if (var3.isEmpty()) {
            return null;
         } else {
            UseEvents.Context var4 = (UseEvents.Context)var3.get();
            this.onUse(var4, UseEvents.interactEvent(var2[0]).orElse(null));
            return null;
         }
      } else {
         return null;
      }
   }

   protected abstract void onUse(UseEvents.Context var1, PlayerInteractEvent var2);

   protected void onUse(Player var1, PlayerInteractEvent var2) {
      this.onUse(new UseEvents.Context(var1, var2 != null ? var1.getInventory().getItemInMainHand() : null, EquipmentSlot.HAND), var2);
   }
}
