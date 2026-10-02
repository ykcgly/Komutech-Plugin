package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class StorageDetectorScript implements NativeScript {
   private static final String MACHINE_ID = "KOMUTECH_JZ_GJ_存储检测器";
   private static final StorageDetectorScript.Direction[] DIRECTIONS = new StorageDetectorScript.Direction[]{
      new StorageDetectorScript.Direction("东", 1, 0),
      new StorageDetectorScript.Direction("南", 0, 1),
      new StorageDetectorScript.Direction("西", -1, 0),
      new StorageDetectorScript.Direction("北", 0, -1)
   };
   private final Map<String, Long> checkTimeCache = new ConcurrentHashMap<>();
   private final Map<String, Long> reportTimeCache = new ConcurrentHashMap<>();
   private final Map<String, Integer> facingCache = new ConcurrentHashMap<>();
   private final Map<String, Long> playerTimeCache = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("tick".equals(var1)) {
         MachineScriptHelper.MachineContext var4 = MachineScriptHelper.parse(var2[0]);
         if (var4 != null && "KOMUTECH_JZ_GJ_存储检测器".equals(MachineScriptHelper.getMachineId(var4.machine()))) {
            this.handleStone(var4.location());
         }
      } else if ("onPlace".equals(var1) && var2[0] instanceof BlockPlaceEvent var3) {
         Location var9 = var3.getBlock().getLocation();
         this.clearCache(var9);
         Player var10 = var3.getPlayer();
         var10.sendMessage(ChatColor.GREEN + "已放置存储检测器");
         var10.sendMessage(ChatColor.YELLOW + "功能: 检测东南西北四个方向的粘液机器");
         var10.sendMessage(ChatColor.GRAY + "站上机器并面向不同方向查看详细信息");
         this.handleStone(var9);
      } else if ("onBreak".equals(var1) && var2.length >= 1) {
         Location var8 = extractLocation(var2[0]);
         if (var8 != null) {
            this.clearCache(var8);
            if (var2[0] instanceof BlockBreakEvent var5) {
               var5.getPlayer().sendMessage(ChatColor.GOLD + "存储检测器已被拆除");
            }
         }
      }

      return null;
   }

   private void handleStone(Location var1) {
      long var2 = System.currentTimeMillis();
      String var4 = locationKey(var1);
      if (var2 - this.checkTimeCache.getOrDefault(var4, 0L) >= 200L) {
         this.checkTimeCache.put(var4, var2);
         World var5 = var1.getWorld();
         if (var5 != null) {
            Player var6 = this.getPlayerOnStone(var1, var5, var2);
            List var7 = this.checkDirections(var1);
            if (var6 != null) {
               this.handlePlayer(var1, var6, var7, var2);
            } else if (var2 - this.reportTimeCache.getOrDefault(var4, 0L) >= 10000L) {
               this.sendBriefReport(var1, var5, var7);
               this.reportTimeCache.put(var4, var2);
            }
         }
      }
   }

   private Player getPlayerOnStone(Location var1, World var2, long var3) {
      Location var5 = var1.clone().add(0.5, 1.0, 0.5);

      for (Player var7 : var2.getPlayers()) {
         Location var8 = var7.getLocation();
         if (var8.getWorld().equals(var2)
            && Math.abs(var8.getX() - var5.getX()) < 0.7
            && Math.abs(var8.getZ() - var5.getZ()) < 0.7
            && Math.abs(var8.getY() - var5.getY()) < 1.5) {
            String var9 = var7.getUniqueId().toString();
            long var10 = this.playerTimeCache.getOrDefault(var9, 0L);
            this.playerTimeCache.put(var9, var3);
            if (var3 - var10 > 1000L) {
               return var7;
            }
         }
      }

      return null;
   }

   private List<StorageDetectorScript.DirectionInfo> checkDirections(Location var1) {
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < DIRECTIONS.length; var3++) {
         StorageDetectorScript.Direction var4 = DIRECTIONS[var3];
         Location var5 = var1.clone().add(var4.offsetX, 0.0, var4.offsetZ);
         SlimefunItem var6 = MachineScriptHelper.getSfItem(var5);
         var2.add(
            new StorageDetectorScript.DirectionInfo(
               var4.name, var3, var6 == null ? null : var6.getId(), var6 == null ? "无机器" : machineName(var6), var6 == null ? null : this.checkStorage(var5)
            )
         );
      }

      return var2;
   }

   private StorageDetectorScript.StorageInfo checkStorage(Location var1) {
      Object var2 = MachineScriptHelper.getMenu(var1);
      if (var2 == null) {
         return null;
      } else {
         ItemStack[] var3 = MachineScriptHelper.getMenuContents(var2);
         boolean var4 = false;
         int var5 = 0;
         int var6 = 0;
         int var7 = 0;
         ArrayList var8 = new ArrayList();

         for (ItemStack var12 : var3) {
            if (var12 != null && !var12.getType().isAir()) {
               var4 = true;
               var5 += var12.getAmount();
               var6++;
               var7++;
               String var13 = formatItemName(var12);
               var8.add(new StorageDetectorScript.ItemSummary(var13, var12.getAmount()));
            }
         }

         return new StorageDetectorScript.StorageInfo(var4, var5, var6, var7, var3.length, var8);
      }
   }

   private void handlePlayer(Location var1, Player var2, List<StorageDetectorScript.DirectionInfo> var3, long var4) {
      Vector var6 = var2.getEyeLocation().getDirection();
      int var7 = getDirection(var6.getX(), var6.getZ());
      String var8 = var2.getUniqueId() + "_" + locationKey(var1);
      int var9 = this.facingCache.getOrDefault(var8, -1);
      boolean var10 = var9 != var7;
      this.facingCache.put(var8, var7);
      String var11 = locationKey(var1);
      if (var10 || var4 - this.reportTimeCache.getOrDefault(var11, 0L) >= 2000L) {
         this.sendDetailedReport(var2, (StorageDetectorScript.DirectionInfo)var3.get(var7), var10);
         this.reportTimeCache.put(var11, var4);
      }
   }

   private static int getDirection(double var0, double var2) {
      return Math.abs(var0) > Math.abs(var2) ? (var0 > 0.0 ? 0 : 2) : (var2 > 0.0 ? 1 : 3);
   }

   private void sendDetailedReport(Player var1, StorageDetectorScript.DirectionInfo var2, boolean var3) {
      if (var3) {
         var1.sendMessage(ChatColor.GOLD + "=== 您正面向" + var2.direction + "方 ===");
         var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 1.2F);
      }

      if (var2.machineId == null) {
         var1.sendMessage(ChatColor.YELLOW + var2.direction + "方: " + ChatColor.RED + "没有检测到粘液机器");
         var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3F, 0.8F);
      } else {
         StringBuilder var4 = new StringBuilder();
         var4.append(ChatColor.YELLOW)
            .append(var2.direction)
            .append("方机器: ")
            .append(ChatColor.AQUA)
            .append(ChatColor.stripColor(var2.machineName))
            .append("\n");
         if (var2.storageInfo != null) {
            StorageDetectorScript.StorageInfo var5 = var2.storageInfo;
            if (var5.hasItems) {
               int var6 = var5.totalSlots > 0 ? Math.round(var5.usedSlots * 100.0F / var5.totalSlots) : 0;
               var4.append(ChatColor.WHITE)
                  .append("存储: ")
                  .append(ChatColor.GREEN)
                  .append(var5.usedSlots)
                  .append("/")
                  .append(var5.totalSlots)
                  .append("槽位 (")
                  .append(var6)
                  .append("%)\n");
               var4.append(ChatColor.WHITE).append("物品: ").append(ChatColor.YELLOW).append(var5.itemCount).append("个, ").append(var5.itemTypes).append("种\n");
               if (!var5.items.isEmpty()) {
                  var4.append(ChatColor.WHITE).append("主要物品:\n");

                  for (int var7 = 0; var7 < Math.min(var5.items.size(), 5); var7++) {
                     StorageDetectorScript.ItemSummary var8 = var5.items.get(var7);
                     var4.append(ChatColor.GRAY)
                        .append("  • ")
                        .append(ChatColor.WHITE)
                        .append(var8.name)
                        .append(ChatColor.GRAY)
                        .append(" x")
                        .append(var8.amount)
                        .append("\n");
                  }

                  if (var5.items.size() > 5) {
                     var4.append(ChatColor.GRAY).append("  等").append(var5.items.size() - 5).append("种物品\n");
                  }
               }
            } else {
               var4.append(ChatColor.GRAY).append("存储: 空\n");
            }
         } else {
            var4.append(ChatColor.DARK_GRAY).append("该机器没有存储空间\n");
         }

         var1.sendMessage(var4.toString());
         if (var2.storageInfo != null && var2.storageInfo.hasItems) {
            int var9 = var2.storageInfo.totalSlots > 0 ? Math.round(var2.storageInfo.usedSlots * 100.0F / var2.storageInfo.totalSlots) : 0;
            var1.playSound(var1.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3F, 0.5F + var9 / 100.0F * 0.5F);
         }
      }
   }

   private void sendBriefReport(Location var1, World var2, List<StorageDetectorScript.DirectionInfo> var3) {
      for (Player var5 : var2.getPlayers()) {
         if (!(var5.getLocation().distance(var1) > 15.0)) {
            StringBuilder var6 = new StringBuilder();
            var6.append(ChatColor.GOLD).append("=== 存储检测器 - 四向机器检测 ===\n");
            boolean var7 = false;

            for (int var8 = 0; var8 < var3.size(); var8++) {
               StorageDetectorScript.DirectionInfo var9 = (StorageDetectorScript.DirectionInfo)var3.get(var8);
               var6.append(ChatColor.YELLOW).append(var9.direction).append(ChatColor.WHITE).append("方: ");
               if (var9.machineId != null) {
                  var7 = true;
                  var6.append(ChatColor.AQUA).append(ChatColor.stripColor(var9.machineName)).append(" ");
                  if (var9.storageInfo != null) {
                     var6.append(var9.storageInfo.hasItems ? ChatColor.GREEN + "[" + var9.storageInfo.itemCount + "个物品]" : ChatColor.GRAY + "[空]");
                  } else {
                     var6.append(ChatColor.DARK_GRAY).append("[无存储]");
                  }
               } else {
                  var6.append(ChatColor.RED).append("无机器");
               }

               if (var8 < var3.size() - 1) {
                  var6.append("\n");
               }
            }

            var6.append(ChatColor.GRAY).append("\n").append(ChatColor.ITALIC).append("站上机器并面向不同方向查看详细信息");
            var5.sendMessage(var6.toString());
            if (var7) {
               var5.playSound(var5.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3F, 1.0F);
            }
         }
      }
   }

   private static String machineName(SlimefunItem var0) {
      String var1 = var0.getItemName();
      return var1 != null && !var1.isBlank() ? var1 : var0.getId();
   }

   private static String formatItemName(ItemStack var0) {
      ItemMeta var1 = var0.getItemMeta();
      String var2 = var1 != null && var1.hasDisplayName() ? var1.getDisplayName() : formatMaterialName(var0.getType().name());
      return var2.length() > 20 ? var2.substring(0, 17) + "..." : var2;
   }

   private static String formatMaterialName(String var0) {
      String[] var1 = var0.toLowerCase(Locale.ROOT).split("_");
      StringBuilder var2 = new StringBuilder();

      for (String var6 : var1) {
         if (!var6.isEmpty()) {
            if (!var2.isEmpty()) {
               var2.append(' ');
            }

            var2.append(Character.toUpperCase(var6.charAt(0))).append(var6.substring(1));
         }
      }

      return var2.toString();
   }

   private void clearCache(Location var1) {
      String var2 = locationKey(var1);
      this.checkTimeCache.remove(var2);
      this.reportTimeCache.remove(var2);
   }

   private static String locationKey(Location var0) {
      return var0.getWorld().getName() + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   private static Location extractLocation(Object var0) {
      if (var0 instanceof BlockBreakEvent var2) {
         return var2.getBlock().getLocation();
      } else {
         return var0 instanceof BlockPlaceEvent var1 ? var1.getBlock().getLocation() : null;
      }
   }

   private record Direction(String name, int offsetX, int offsetZ) {
   }

   private record DirectionInfo(String direction, int index, String machineId, String machineName, StorageDetectorScript.StorageInfo storageInfo) {
   }

   private record ItemSummary(String name, int amount) {
   }

   private record StorageInfo(boolean hasItems, int itemCount, int itemTypes, int usedSlots, int totalSlots, List<StorageDetectorScript.ItemSummary> items) {
   }
}
