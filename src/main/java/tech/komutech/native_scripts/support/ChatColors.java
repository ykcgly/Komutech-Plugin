package tech.komutech.native_scripts.support;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class ChatColors {
   private ChatColors() {
   }

   public static String translate(String var0) {
      return var0 == null ? "" : ChatColor.translateAlternateColorCodes('&', var0);
   }

   public static void send(Player var0, String var1) {
      if (var0 != null && var1 != null) {
         var0.sendMessage(translate(var1));
      }
   }
}
