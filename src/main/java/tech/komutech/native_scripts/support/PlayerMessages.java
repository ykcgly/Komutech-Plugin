package tech.komutech.native_scripts.support;

import org.bukkit.entity.Player;

public final class PlayerMessages {
   private PlayerMessages() {
   }

   public static void send(Player var0, String var1) {
      if (var0 != null && var1 != null) {
         var0.sendMessage(var1);
      }
   }
}
