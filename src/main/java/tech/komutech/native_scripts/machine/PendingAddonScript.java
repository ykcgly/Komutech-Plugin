package tech.komutech.native_scripts.machine;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;

public final class PendingAddonScript implements NativeScript {
   private static final String PLACE_MESSAGE = "想用看说明";
   private static final String OPEN_MESSAGE = "想用看说明";
   private final PendingAddonScript.Mode mode;

   public PendingAddonScript(PendingAddonScript.Mode var1) {
      this.mode = var1;
   }

   public static PendingAddonScript onPlace() {
      return new PendingAddonScript(PendingAddonScript.Mode.PLACE);
   }

   public static PendingAddonScript onOpen() {
      return new PendingAddonScript(PendingAddonScript.Mode.OPEN);
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if (this.mode == PendingAddonScript.Mode.PLACE && "onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         Player var7 = var3.getPlayer();
         KomutechSupport.send(var7, "想用看说明");
      } else if (this.mode == PendingAddonScript.Mode.OPEN && "onOpen".equals(var1) && var2[0] instanceof Player var4) {
         KomutechSupport.send(var4, "想用看说明");
      }

      return null;
   }

   public static enum Mode {
      PLACE,
      OPEN;
   }
}
