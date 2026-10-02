package tech.komutech.native_scripts.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.komutech.native_scripts.NativeScriptRegistry;
import tech.komutech.native_scripts.cultivation.PuTuanScript;
import tech.komutech.commands.NativeAddonCommand;

public final class KomutechAdminCommands {
   private static final Pattern SPIRIT_CURRENT = Pattern.compile("^(\\d+(?:\\.\\d+)?)");

   private KomutechAdminCommands() {
   }

   public static void register() {
      NativeAddonCommand.setExtension(KomutechAdminCommands::handle);
      NativeAddonCommand.setTabExtension(KomutechAdminCommands::tabComplete);
   }

   private static boolean handle(CommandSender var0, String[] var1) {
      if (var1.length == 0) {
         return false;
      } else {
         String var2 = var1[0].toLowerCase(Locale.ROOT);
         if (!"putuan".equals(var2) && !"蒲团".equals(var1[0]) && !"蒲团清理".equals(var1[0])) {
            KomutechAdminCommands.Mode var3 = parseMode(var1[0]);
            if (var3 == null) {
               return false;
            } else if (!var0.hasPermission("komutech.admin")) {
               var0.sendMessage("§c你没有权限执行这个命令。");
               return true;
            } else {
               String var4 = var1[0].toLowerCase(Locale.ROOT);
               if (var1.length < 4) {
                  var0.sendMessage("§c用法: /komutech " + var4 + " <玩家名> <灵气|属性点> <数量>");
                  return true;
               } else {
                  String var5 = var1[1];
                  String var6 = var1[2];
                  String var7 = var1[3];
                  Map var8 = PlayerAttributeStore.load(var5);
                  if (var8 == null) {
                     var0.sendMessage("§c找不到玩家属性数据: §f" + var5);
                     return true;
                  } else {
                     CultivationMath.ensureDataComplete(var5, var8);
                     if ("灵气".equals(var6)) {
                        return adjustSpirit(var0, var5, var8, var7, var3);
                     } else if ("属性点".equals(var6)) {
                        return adjustAttributePoints(var0, var5, var8, var7, var3);
                     } else {
                        var0.sendMessage("§c未知字段: §f" + var6 + " §7(仅支持 灵气 / 属性点)");
                        return true;
                     }
                  }
               }
            }
         } else {
            return handlePutuan(var0, var1);
         }
      }
   }

   private static boolean handlePutuan(CommandSender var0, String[] var1) {
      if (!var0.hasPermission("komutech.admin")) {
         var0.sendMessage("§c你没有权限执行这个命令。");
         return true;
      } else {
         PuTuanScript var2 = NativeScriptRegistry.puTuan();
         if (var2 == null) {
            var0.sendMessage("§c蒲团脚本尚未初始化。");
            return true;
         } else {
            String var3 = var1.length >= 2 ? var1[1].toLowerCase(Locale.ROOT) : "cleanup";
            if ("蒲团清理".equals(var1[0])) {
               var3 = "cleanup";
            }

            switch (var3) {
               case "cleanup":
               case "all":
               case "sweep":
               case "清理":
                  int var12 = var2.adminSweepAll();
                  var0.sendMessage("§a[蒲团] 已扫描清理残留修炼显示，移除 §f" + var12 + " §a个实体。");
                  return true;
               case "here":
               case "这里":
                  if (var0 instanceof Player var11) {
                     Block var13 = var11.getTargetBlockExact(6);
                     Location var8 = var13 != null ? var13.getLocation() : var11.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
                     var2.adminCleanupAt(var8);
                     var0.sendMessage("§a[蒲团] 已强制清理坐标 §f" + var8.getBlockX() + "," + var8.getBlockY() + "," + var8.getBlockZ());
                     return true;
                  }

                  var0.sendMessage("§c仅玩家可使用 here。");
                  return true;
               case "near":
               case "附近":
                  if (var0 instanceof Player var6) {
                     double var7 = 16.0;
                     if (var1.length >= 3) {
                        try {
                           var7 = Double.parseDouble(var1[2]);
                        } catch (NumberFormatException var10) {
                        }
                     }

                     int var9 = var2.adminCleanupNearby(var6.getLocation(), Math.max(1.0, Math.min(var7, 64.0)));
                     var0.sendMessage("§a[蒲团] 附近清理完成，移除 §f" + var9 + " §a个残留实体。");
                     return true;
                  }

                  var0.sendMessage("§c仅玩家可使用 near。");
                  return true;
               default:
                  var0.sendMessage("§c用法: /komutech putuan <cleanup|here|near [半径]>");
                  return true;
            }
         }
      }
   }

   private static KomutechAdminCommands.Mode parseMode(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "set" -> KomutechAdminCommands.Mode.SET;
         case "add" -> KomutechAdminCommands.Mode.ADD;
         case "take" -> KomutechAdminCommands.Mode.TAKE;
         default -> null;
      };
   }

   private static String modeVerb(KomutechAdminCommands.Mode var0) {
      return switch (var0) {
         case SET -> "设置";
         case ADD -> "增加";
         case TAKE -> "扣除";
      };
   }

   private static double applyAmount(double var0, double var2, KomutechAdminCommands.Mode var4) {
      return switch (var4) {
         case SET -> var2;
         case ADD -> var0 + var2;
         case TAKE -> var0 - Math.abs(var2);
      };
   }

   private static int applyAmount(int var0, int var1, KomutechAdminCommands.Mode var2) {
      return switch (var2) {
         case SET -> var1;
         case ADD -> var0 + var1;
         case TAKE -> var0 - Math.abs(var1);
      };
   }

   private static boolean adjustSpirit(CommandSender var0, String var1, Map<String, Object> var2, String var3, KomutechAdminCommands.Mode var4) {
      double var5;
      try {
         var5 = Double.parseDouble(var3);
      } catch (NumberFormatException var15) {
         var0.sendMessage("§c数量必须是数字");
         return true;
      }

      if (!Double.isFinite(var5)) {
         var0.sendMessage("§c数量无效");
         return true;
      } else {
         double var7 = parseSpiritCurrent(PlayerAttributeStore.getString(var2, "灵气", "0"));
         double var9 = applyAmount(var7, var5, var4);
         if (var9 < 0.0) {
            var9 = 0.0;
         }

         long var11 = (long)Math.floor(var9);
         CultivationMath.CultivationInfo var13 = CultivationMath.getCultivationInfo(var11);
         String var14 = formatSpirit(var9, var13.maxStr());
         var2.put("灵气", var14);
         var2.put("修为", var13.stage());
         syncLingliBase(var2);
         var2.put("灵气获取_实际", AttributePointLimits.spiritGainPercent(var2, PlayerAttributeStore.getInt(var2, "灵气获取", 0)));
         if (!PlayerAttributeStore.save(var1, var2)) {
            var0.sendMessage("§c保存失败: §f" + var1);
            return true;
         } else {
            applyIfOnline(var1, var2);
            var0.sendMessage("§a已" + modeVerb(var4) + " §f" + var1 + " §a的灵气为 §e" + var14 + " §7(" + var13.stage() + ")");
            notifyTarget(var1, "§a管理员已调整你的灵气: §e" + var14);
            return true;
         }
      }
   }

   private static boolean adjustAttributePoints(CommandSender var0, String var1, Map<String, Object> var2, String var3, KomutechAdminCommands.Mode var4) {
      int var5;
      try {
         var5 = Integer.parseInt(var3);
      } catch (NumberFormatException var8) {
         var0.sendMessage("§c属性点数量必须是整数");
         return true;
      }

      int var6 = PlayerAttributeStore.getInt(var2, "属性点", 0);
      int var7 = applyAmount(var6, var5, var4);
      if (var7 < 0) {
         var7 = 0;
      }

      var2.put("属性点", var7);
      if (!PlayerAttributeStore.save(var1, var2)) {
         var0.sendMessage("§c保存失败: §f" + var1);
         return true;
      } else {
         var0.sendMessage("§a已" + modeVerb(var4) + " §f" + var1 + " §a的属性点为 §e" + var7);
         notifyTarget(var1, "§a管理员已调整你的属性点: §e" + var7);
         return true;
      }
   }

   private static void syncLingliBase(Map<String, Object> var0) {
      PlayerAttributeStore.Lingli var1 = PlayerAttributeStore.parseLingli(var0.get("灵力"));
      int var2 = CultivationMath.computeBaseMaxLingli(var0);
      var0.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var1.current(), var2, var1.bonus())));
   }

   private static double parseSpiritCurrent(String var0) {
      if (var0 != null && !var0.isBlank()) {
         Matcher var1 = SPIRIT_CURRENT.matcher(var0.trim());
         if (!var1.find()) {
            return 0.0;
         } else {
            try {
               return Double.parseDouble(var1.group(1));
            } catch (NumberFormatException var3) {
               return 0.0;
            }
         }
      } else {
         return 0.0;
      }
   }

   private static String formatSpirit(double var0, String var2) {
      return Math.abs(var0 - Math.rint(var0)) < 1.0E-9 ? (long)Math.rint(var0) + "/" + var2 : String.format(Locale.ROOT, "%.2f/%s", var0, var2);
   }

   private static void applyIfOnline(String var0, Map<String, Object> var1) {
      Player var2 = Bukkit.getPlayerExact(var0);
      if (var2 != null && var2.isOnline()) {
         AttributeApplier.applyPlayerAttributes(var2, var1);
         PlayerAttributeStore.save(var0, var1);
      }
   }

   private static void notifyTarget(String var0, String var1) {
      Player var2 = Bukkit.getPlayerExact(var0);
      if (var2 != null && var2.isOnline()) {
         var2.sendMessage(var1);
      }
   }

   private static List<String> tabComplete(CommandSender var0, String[] var1) {
      if (!var0.hasPermission("komutech.admin")) {
         return List.of();
      } else {
         String var2 = var1[var1.length - 1].toLowerCase(Locale.ROOT);
         if (var1.length == 1) {
            return filter(List.of("reload", "set", "add", "take", "putuan", "蒲团"), var2);
         } else {
            String var3 = var1[0].toLowerCase(Locale.ROOT);
            if ("putuan".equals(var3) || "蒲团".equals(var1[0])) {
               return var1.length == 2 ? filter(List.of("cleanup", "here", "near"), var2) : List.of();
            } else if (!"set".equals(var3) && !"add".equals(var3) && !"take".equals(var3)) {
               return List.of();
            } else if (var1.length != 2) {
               if (var1.length == 3) {
                  return filter(List.of("灵气", "属性点"), var2);
               } else {
                  return var1.length == 4 ? filter(List.of("0", "1", "10", "100", "1000"), var2) : List.of();
               }
            } else {
               ArrayList var4 = new ArrayList();

               for (Player var6 : Bukkit.getOnlinePlayers()) {
                  var4.add(var6.getName());
               }

               return filter(var4, var2);
            }
         }
      }
   }

   private static List<String> filter(List<String> var0, String var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : var0) {
         if (var4.toLowerCase(Locale.ROOT).startsWith(var1) || var4.startsWith(var1)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static enum Mode {
      SET,
      ADD,
      TAKE;
   }
}
