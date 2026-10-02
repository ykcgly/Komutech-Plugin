package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class YuGanSeedScript implements NativeScript {
   private static final long SEED_TO_SAPLING_MS = 3000L;
   private static final long SAPLING_TO_BAMBOO_MS = 5000L;
   private static final String TARGET_PDC_ID = "KOMUTECH_L_DJ_XSDLY";
   private static final String PRODUCT_ID = "KOMUTECH_L_LZ_YG";
   private static final int MAX_PRODUCTS = 16;
   private static final long TICK_INTERVAL_MS = 1000L;
   private static final Map<Location, YuGanSeedScript.SeedState> STATE_MAP = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> PDC_CACHE = new ConcurrentHashMap<>();
   private static SlimefunItem productItem;

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("tick".equals(var1)) {
         MachineScriptHelper.MachineContext var4 = MachineScriptHelper.parse(var2[0]);
         if (var4 != null) {
            tick(var4);
         }
      } else if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         STATE_MAP.put(var3.getBlock().getLocation(), YuGanSeedScript.SeedState.create());
      } else if ("onBreak".equals(var1)) {
         handleBreak(var2);
      }

      return null;
   }

   private static void tick(MachineScriptHelper.MachineContext var0) {
      Location var1 = var0.location();
      World var2 = var1.getWorld();
      if (var2 != null) {
         YuGanSeedScript.SeedState var3 = STATE_MAP.computeIfAbsent(var1, var0x -> YuGanSeedScript.SeedState.create());
         long var4 = System.currentTimeMillis();
         if (var4 - var3.lastCheck >= 1000L) {
            var3.lastCheck = var4;
            if (var2.getBlockAt(var1.getBlockX(), var1.getBlockY() - 1, var1.getBlockZ()).getType() == Material.GRASS_BLOCK) {
               Block var6 = var0.block();
               if (var3.stage == 0) {
                  if (var3.lastTime == 0L) {
                     var3.lastTime = var4;
                  }

                  if (var4 - var3.lastTime >= 3000L) {
                     var6.setType(Material.BAMBOO_SAPLING);
                     var3.stage = 1;
                     var3.lastTime = var4;
                  }
               } else if (var3.stage == 1 && var4 - var3.lastTime >= 5000L) {
                  var3.pdcCount = consumePdcBlocks(var1);
                  var6.setType(Material.BAMBOO);
                  var3.stage = 2;
               }
            }
         }
      }
   }

   private static int consumePdcBlocks(Location var0) {
      World var1 = var0.getWorld();
      if (var1 == null) {
         return 0;
      } else {
         int var2 = var0.getBlockX();
         int var3 = var0.getBlockY();
         int var4 = var0.getBlockZ();
         int var5 = 0;
         int[][] var6 = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

         for (int[] var10 : var6) {
            Block var11 = var1.getBlockAt(var2 + var10[0], var3, var4 + var10[1]);
            if (hasTargetPdc(var11)) {
               var11.setType(Material.POPPY);
               PDC_CACHE.remove(var11.getLocation().toString());
               var5++;
            }
         }

         return var5;
      }
   }

   private static boolean hasTargetPdc(Block var0) {
      String var1 = var0.getLocation().toString();
      Boolean var2 = PDC_CACHE.get(var1);
      if (var2 != null) {
         return var2;
      } else {
         boolean var3 = false;

         try {
            if (!(var0.getState() instanceof TileState var5)) {
               PDC_CACHE.put(var1, false);
               return false;
            }

            PersistentDataContainer var6 = var5.getPersistentDataContainer();

            for (NamespacedKey var8 : var6.getKeys()) {
               String var9 = (String)var6.get(var8, PersistentDataType.STRING);
               if (var9 != null && var9.contains("KOMUTECH_L_DJ_XSDLY")) {
                  var3 = true;
                  break;
               }
            }
         } catch (RuntimeException var10) {
         }

         PDC_CACHE.put(var1, var3);
         return var3;
      }
   }

   private static int getBambooHeight(Location var0) {
      World var1 = var0.getWorld();
      if (var1 == null) {
         return 0;
      } else {
         int var2 = var0.getBlockX();
         int var3 = var0.getBlockY();
         int var4 = var0.getBlockZ();
         int var5 = 0;

         for (int var6 = var3; var6 < var1.getMaxHeight() && var1.getBlockAt(var2, var6, var4).getType() == Material.BAMBOO; var6++) {
            var5++;
         }

         return var5;
      }
   }

   private static void handleBreak(Object[] var0) {
      try {
         Object var1 = var0[0];
         Location var2 = ((Block)var1.getClass().getMethod("getBlock").invoke(var1)).getLocation();
         YuGanSeedScript.SeedState var3 = STATE_MAP.remove(var2);
         if (!(var3 != null && var3.stage == 2 && var0.length >= 3 && var0[2] instanceof Collection var4)) {
            return;
         }

         if (productItem == null) {
            productItem = SlimefunItem.getById("KOMUTECH_L_LZ_YG");
         }

         if (productItem == null) {
            return;
         }

         int var9 = getBambooHeight(var2);
         if (var9 <= 0) {
            return;
         }

         int var6 = (int)Math.floor(var9 * (1.0 + var3.pdcCount * 0.25));
         var6 = Math.min(Math.max(var6, 1), 16);
         var4.add(productItem.getItem().clone().asQuantity(var6));
      } catch (ReflectiveOperationException var8) {
      }
   }

   private static final class SeedState {
      int stage;
      long lastTime;
      int pdcCount;
      long lastCheck;

      static YuGanSeedScript.SeedState create() {
         return new YuGanSeedScript.SeedState();
      }
   }
}
