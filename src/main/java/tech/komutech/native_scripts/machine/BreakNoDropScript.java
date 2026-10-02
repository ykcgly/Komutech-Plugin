package tech.komutech.native_scripts.machine;

import org.bukkit.event.block.BlockBreakEvent;
import tech.komutech.native_scripts.NativeScript;

public final class BreakNoDropScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var3) {
         var3.setDropItems(false);
      }

      return null;
   }
}
