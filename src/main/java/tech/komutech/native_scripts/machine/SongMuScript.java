package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class SongMuScript implements NativeScript {
   private static final int STATE_GROWING = 0;
   private static final int STATE_MATURE = 1;
   private static final int STATE_DAMAGED = 2;
   private static final long GROWTH_TIME_MS = 8000L;
   private static final long DAMAGE_DELAY_MS = 2000L;
   private static final Material REQUIRED_BELOW = Material.CAMPFIRE;
   private static final Material REQUIRED_ABOVE = Material.SMOKER;
   private static final Material MATURE_BLOCK = Material.COAL_BLOCK;
   private static final Material DAMAGED_BLOCK = Material.GRAY_WOOL;
   private static final String REQUIRED_ITEM_ID = "KOMUTECH_L_JCWP_JZJS";
   private static final String DROP_ITEM_ID = "KOMUTECH_L_JCWP_SYM";
   private static final Map<Location, int[]> GROWTH_STATES = new ConcurrentHashMap<>();
   private static SlimefunItem requiredItem;
   private static SlimefunItem dropItem;

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("tick".equals(var1)) {
         MachineScriptHelper.MachineContext var6 = MachineScriptHelper.parse(var2[0]);
         if (var6 != null) {
            tick(var6);
         }
      } else if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         GROWTH_STATES.remove(var3.getBlock().getLocation());
      } else if ("onBreak".equals(var1) && var2.length >= 1) {
         handleSlimefunBreak(var2);
      } else if ("onBlockBreak".equals(var1) && var2[0] instanceof BlockBreakEvent var4) {
         handleBlockBreak(var4);
      } else if ("onBlockPhysics".equals(var1) && var2[0] instanceof BlockPhysicsEvent var5 && GROWTH_STATES.containsKey(var5.getBlock().getLocation())) {
         var5.setCancelled(true);
      }

      return null;
   }

   private static void tick(MachineScriptHelper.MachineContext var0) {
      Location var1 = var0.location();
      World var2 = var1.getWorld();
      if (var2 != null) {
         int var3 = var1.getBlockX();
         int var4 = var1.getBlockY();
         int var5 = var1.getBlockZ();
         if (var2.getBlockAt(var3, var4 - 1, var5).getType() == REQUIRED_BELOW && var2.getBlockAt(var3, var4 + 1, var5).getType() == REQUIRED_ABOVE) {
            int[] var6 = GROWTH_STATES.get(var1);
            long var7 = System.currentTimeMillis();
            if (var6 == null) {
               GROWTH_STATES.put(var1, new int[]{0, (int)var7});
            } else if (var6[0] == 0) {
               long var9 = var7 - var6[1];
               Block var11 = var2.getBlockAt(var3, var4 + 1, var5);
               if (var9 >= 2000L && !checkItem(var11, false)) {
                  var0.block().setType(DAMAGED_BLOCK);
                  var6[0] = 2;
               } else {
                  if (var9 >= 8000L) {
                     if (checkItem(var11, true)) {
                        var0.block().setType(MATURE_BLOCK);
                        var6[0] = 1;
                     } else {
                        var0.block().setType(DAMAGED_BLOCK);
                        var6[0] = 2;
                     }
                  }
               }
            }
         } else {
            GROWTH_STATES.remove(var1);
         }
      }
   }

   private static boolean checkItem(Block var0, boolean var1) {
      if (var0.getState() instanceof Container var2) {
         Inventory var6 = var2.getInventory();
         ItemStack var4 = var6.getItem(0);
         if (var4 != null && !var4.getType().isAir()) {
            if (requiredItem == null) {
               requiredItem = SlimefunItem.getById("KOMUTECH_L_JCWP_JZJS");
            }

            if (requiredItem == null) {
               return false;
            } else {
               SlimefunItem var5 = SlimefunItem.getByItem(var4);
               if (var5 == null || !requiredItem.getId().equals(var5.getId())) {
                  return false;
               } else if (var4.getAmount() < 1) {
                  return false;
               } else {
                  if (var1) {
                     if (var4.getAmount() > 1) {
                        var4.setAmount(var4.getAmount() - 1);
                     } else {
                        var6.clear(0);
                     }
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void handleSlimefunBreak(Object[] var0) {
      try {
         Object var1 = var0[0];
         Location var2 = ((Block)var1.getClass().getMethod("getBlock").invoke(var1)).getLocation();
         int[] var3 = GROWTH_STATES.get(var2);
         if (var3 == null) {
            return;
         }

         var1.getClass().getMethod("setDropItems", boolean.class).invoke(var1, false);
         if (var0.length >= 3 && var0[2] instanceof Collection var4) {
            var4.clear();
         }

         if (var3[0] == 1) {
            dropProduct(var2);
         } else if (var3[0] == 2) {
            var1.getClass().getMethod("setExpToDrop", int.class).invoke(var1, 0);
         }

         GROWTH_STATES.remove(var2);
      } catch (ReflectiveOperationException var6) {
      }
   }

   private static void handleBlockBreak(BlockBreakEvent var0) {
      Location var1 = var0.getBlock().getLocation();
      int[] var2 = GROWTH_STATES.get(var1);
      if (var2 != null) {
         var0.setDropItems(false);
         var0.setExpToDrop(0);
         if (var2[0] == 1) {
            dropProduct(var1);
         }

         GROWTH_STATES.remove(var1);
      }
   }

   private static void dropProduct(Location var0) {
      if (dropItem == null) {
         dropItem = SlimefunItem.getById("KOMUTECH_L_JCWP_SYM");
      }

      if (dropItem != null) {
         int var1 = 1 + ThreadLocalRandom.current().nextInt(3);
         var0.getWorld().dropItemNaturally(var0, dropItem.getItem().clone().asQuantity(var1));
      }
   }
}
