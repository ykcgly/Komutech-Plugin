package tech.komutech.native_scripts.machine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import tech.komutech.native_scripts.NativeScript;

public final class MaterialNoDropScript implements NativeScript {
   private static final Map<Location, String> CACHE = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var6) {
         CACHE.put(var6.getBlock().getLocation(), var6.getBlock().getType().name());
         return null;
      } else if ("onBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var3) {
         Location var8 = var3.getBlock().getLocation();
         String var5 = CACHE.get(var8);
         if (var5 != null && !var5.equals(var3.getBlock().getType().name())) {
            var3.setDropItems(false);
            var3.setExpToDrop(0);
         }

         CACHE.remove(var8);
         return null;
      } else {
         return "tick".equals(var1) ? null : null;
      }
   }
}
