package tech.komutech.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import tech.komutech.KT;

/**
 * {@code /komutech} 命令。
 *
 * <p>与原实现的差异：去掉了 {@code ProjectAddon} 物品计数展示与 RSC 共存模式提示
 * （新形态是单插件自包含，没有附属目录 / 共存桥），其余输出与子命令（reload/set/add/take
 * 扩展点）保持一致。扩展点由 {@link tech.komutech.native_scripts.support.KomutechAdminCommands}
 * 注册。
 */
public final class NativeAddonCommand implements CommandExecutor, TabCompleter {
   private static volatile NativeAddonCommand.Extension extension;
   private static volatile NativeAddonCommand.TabExtension tabExtension;

   private static final String PERM = "komutech.admin";

   public static void setExtension(NativeAddonCommand.Extension var0) {
      extension = var0;
   }

   public static void setTabExtension(NativeAddonCommand.TabExtension var0) {
      tabExtension = var0;
   }

   public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
      String var6 = "口木科技";
      if (var4.length > 0 && var4[0].equalsIgnoreCase("reload")) {
         if (!var1.hasPermission(PERM)) {
            var1.sendMessage("§c你没有权限执行这个命令。");
            return true;
         }
         var1.sendMessage("§c独立版不支持热重载：内容 yml 已打进 jar，请重启服务器。");
         return true;
      }

      NativeAddonCommand.Extension var12 = extension;
      if (var12 != null && var4.length > 0 && var12.onCommand(var1, var4)) {
         return true;
      }

      Plugin var8 = KT.plugin();
      if (var8 == null) {
         var1.sendMessage("§c[" + var6 + "] 插件尚未初始化。");
         return true;
      }
      var1.sendMessage("§b§l" + var6);
      var1.sendMessage("§7插件版本: §fKomutech §e" + var8.getDescription().getVersion());
      var1.sendMessage("§7运行模式: §f纯 Java 独立运行");
      var1.sendMessage("§7服务端: §f" + Bukkit.getBukkitVersion());
      if (var1.hasPermission(PERM) && extension != null) {
         var1.sendMessage("§7管理: §f/komutech set|add|take <玩家> 灵气|属性点 <数量>");
      }
      return true;
   }

   public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
      NativeAddonCommand.TabExtension var5 = tabExtension;
      if (var5 != null && var1.hasPermission(PERM)) {
         List<String> var6 = var5.onTabComplete(var1, var4);
         if (var6 != null && !var6.isEmpty()) {
            return var6;
         }
      }

      if (var4.length == 1 && var1.hasPermission(PERM)) {
         ArrayList<String> var11 = new ArrayList<>();
         String var7 = var4[0].toLowerCase(Locale.ROOT);
         for (String var10 : extension != null ? List.of("set", "add", "take") : List.<String>of()) {
            if (var10.startsWith(var7)) {
               var11.add(var10);
            }
         }
         return var11;
      }
      return Collections.emptyList();
   }

   @FunctionalInterface
   public interface Extension {
      boolean onCommand(CommandSender var1, String[] var2);
   }

   @FunctionalInterface
   public interface TabExtension {
      List<String> onTabComplete(CommandSender var1, String[] var2);
   }
}
