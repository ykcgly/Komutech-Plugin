package tech.komutech.native_scripts.tool;

import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;

public final class PdcInspectorScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         this.handlePlace(var3);
      }

      return null;
   }

   private void handlePlace(BlockPlaceEvent var1) {
      Player var2 = var1.getPlayer();
      Block var3 = var1.getBlockPlaced().getLocation().subtract(0.0, 1.0, 0.0).getBlock();
      if (var3.getType().isAir()) {
         KomutechSupport.send(var2, "§c下方无方块或为空气");
      } else if (var3.getState() instanceof TileState var5) {
         PersistentDataContainer var6 = var5.getPersistentDataContainer();
         KomutechSupport.send(var2, "§6===== 方块PDC信息 =====");
         KomutechSupport.send(var2, "§7方块类型: §f" + var3.getType().name());
         Set<NamespacedKey> var7 = var6.getKeys();
         boolean var8 = false;

         for (NamespacedKey var10 : var7) {
            String var11 = (String)var6.get(var10, PersistentDataType.STRING);
            if (var11 != null) {
               KomutechSupport.send(var2, "§e" + var10 + ": §f" + var11);
               var8 = true;
            }
         }

         if (!var8) {
            KomutechSupport.send(var2, "§7该方块没有PDC数据");
         }
      } else {
         KomutechSupport.send(var2, "§7该方块不支持PDC");
      }
   }
}
