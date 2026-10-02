package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Set;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.MachineMenuGuard;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class KomutechBlockMachineMenuGuard implements NativeLifecycleScript, KomutechMenuHandler {
   private static final Set<String> SCRIPTED_MACHINE_IDS = Set.of(
      "KOMUTECH_L_ZJ_萬衍儀", "KOMUTECH_L_ZJ_終極合成台", "KOMUTECH_L_ZJ_終極合成台核心", "KOMUTECH_L_JQ_灵脉宝窟", "KOMUTECH_L_JQ_灵脉晶辉宝窟", "KOMUTECH_L_WD_WXZZY"
   );

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return null;
   }

   @Override
   public boolean handles(InventoryView var1) {
      SlimefunItem var2 = resolveMachine(var1);
      if (var2 == null) {
         return false;
      } else {
         String var3 = var2.getId();
         return var3.startsWith("KOMUTECH_") && !SCRIPTED_MACHINE_IDS.contains(var3);
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (this.handles(var1.getView())) {
         MachineMenuGuard.protectTopInventory(var1, var1x -> isAllowedSlot(var1.getView(), var1x));
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         MachineMenuGuard.protectTopInventoryDrag(var1, var1x -> isAllowedSlot(var1.getView(), var1x));
      }
   }

   private static boolean isAllowedSlot(InventoryView var0, int var1) {
      SlimefunItem var2 = resolveMachine(var0);
      return var2 == null ? false : MachineScriptHelper.isMachineInventorySlot(var2, var1);
   }

   private static SlimefunItem resolveMachine(InventoryView var0) {
      return MachineScriptHelper.resolveBlockMachine(var0);
   }
}
