package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public final class AdminToolsSupport {
   public static final String POS_KEY_FIRST = "§6§l位置一：";
   public static final String POS_KEY_SECOND = "§6§l位置二：";
   private static final Pattern POS_PATTERN = Pattern.compile("X:(-?\\d+)\\s+Y:(-?\\d+)\\s+Z:(-?\\d+)");
   private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap();

   private AdminToolsSupport() {
   }

   public static boolean checkCooldown(Player var0, long var1) {
      long var3 = System.currentTimeMillis();
      Long var5 = (Long)cooldowns.get(var0.getUniqueId());
      if (var5 != null && var3 - var5 < var1) {
         return false;
      } else {
         cooldowns.put(var0.getUniqueId(), var3);
         return true;
      }
   }

   public static boolean hasPermission(Player var0, Location var1) {
      if (!var0.hasPermission("slimefun.inventory.bypass") && !var0.hasPermission("komutech.hologram.bypass") && !var0.isOp()) {
         return Slimefun.getProtectionManager().hasPermission(var0, var1, Interaction.BREAK_BLOCK) && Slimefun.getProtectionManager().hasPermission(var0, var1, Interaction.INTERACT_BLOCK);
      } else {
         return true;
      }
   }

   public static Block rayTraceBlock(Player var0, double var1) {
      RayTraceResult var3 = var0.getWorld().rayTraceBlocks(var0.getEyeLocation(), var0.getEyeLocation().getDirection(), var1, FluidCollisionMode.NEVER, true);
      return var3 == null ? null : var3.getHitBlock();
   }

   public static Entity rayTraceEntity(Player var0, double var1, Predicate<Entity> var3) {
      RayTraceResult var4 = var0.getWorld().rayTraceEntities(var0.getEyeLocation(), var0.getEyeLocation().getDirection(), var1, 0.1, var3);
      return var4 == null ? null : var4.getHitEntity();
   }

   public static BlockPos parsePos(List<String> var0, String var1) {
      if (var0 == null) {
         return null;
      } else {
         for(String var3 : var0) {
            if (var3 != null && var3.startsWith(var1)) {
               Matcher var4 = POS_PATTERN.matcher(var3.substring(var1.length()).trim());
               if (var4.find()) {
                  return new BlockPos(Integer.parseInt(var4.group(1)), Integer.parseInt(var4.group(2)), Integer.parseInt(var4.group(3)));
               }
            }
         }

         return null;
      }
   }

   public static void writePos(ItemStack var0, String var1, String var2, BlockPos var3) {
      ItemMeta var4 = var0.getItemMeta();
      if (var4 != null && var3 != null) {
         ArrayList<String> var5 = var4.hasLore() ? new ArrayList<>(var4.getLore()) : new ArrayList<>();
         String var6 = var1 + "X:" + var3.x() + " Y:" + var3.y() + " Z:" + var3.z();
         boolean var7 = false;

         for(int var8 = 0; var8 < var5.size(); ++var8) {
            if (((String)var5.get(var8)).startsWith(var1)) {
               var5.set(var8, var6);
               var7 = true;
               break;
            }
         }

         if (!var7) {
            var5.add(var6);
         }

         var4.setLore(var5);
         var0.setItemMeta(var4);
      }
   }

   public static String loreMode(List<String> var0, String var1, String var2, String var3) {
      if (var0 == null) {
         return var2;
      } else {
         for(String var5 : var0) {
            if (var5 != null && var5.contains(var1)) {
               return var5.contains(var3) ? "range" : "single";
            }
         }

         return var2;
      }
   }

   public static void switchLoreMode(ItemStack var0, String var1, String var2, String var3) {
      ItemMeta var4 = var0.getItemMeta();
      if (var4 != null) {
         ArrayList<String> var5 = var4.hasLore() ? new ArrayList<>(var4.getLore()) : new ArrayList<>();
         boolean var6 = "range".equals(loreMode(var5, var1, "single", "范围"));
         var5.removeIf((var1x) -> var1x != null && var1x.contains(var1));
         var5.add(var6 ? var2 : var3);
         var4.setLore(var5);
         var0.setItemMeta(var4);
      }
   }

   public static boolean isSlimefunItem(ItemStack var0, String var1) {
      SlimefunItem var2 = SlimefunItem.getByItem(var0);
      return var2 != null && var1.equals(var2.getId());
   }

   public static ItemStack findInventoryItem(Player var0, String var1) {
      for(ItemStack var5 : var0.getInventory().getContents()) {
         if (var5 != null && isSlimefunItem(var5, var1)) {
            return var5;
         }
      }

      return null;
   }

   public static int takeSimilar(Player var0, ItemStack var1, int var2) {
      int var3 = 0;

      for(int var4 = 0; var4 < 36 && var3 < var2; ++var4) {
         ItemStack var5 = var0.getInventory().getItem(var4);
         if (var5 != null && !var5.getType().isAir() && var5.isSimilar(var1)) {
            int var6 = Math.min(var5.getAmount(), var2 - var3);
            var5.setAmount(var5.getAmount() - var6);
            if (var5.getAmount() <= 0) {
               var0.getInventory().setItem(var4, (ItemStack)null);
            }

            var3 += var6;
         }
      }

      return var3;
   }

   public static Object clickedBlock(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            Object var1 = var0.getClass().getMethod("getClickedBlock").invoke(var0);
            return var1 == null ? null : var1.getClass().getMethod("get").invoke(var1);
         } catch (ReflectiveOperationException var2) {
            return null;
         }
      }
   }

   public static record BlockPos(int x, int y, int z) {
   }
}
