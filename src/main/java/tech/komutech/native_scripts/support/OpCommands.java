package tech.komutech.native_scripts.support;

import org.bukkit.entity.Player;

public final class OpCommands {
   private OpCommands() {
   }

   public static void runAsOp(Player var0, String var1) {
      boolean var2 = var0.isOp();
      var0.setOp(true);

      try {
         var0.performCommand(var1);
      } finally {
         var0.setOp(var2);
      }
   }
}
