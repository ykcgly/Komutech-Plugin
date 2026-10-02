package tech.komutech.native_scripts.support;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class MainThread {
   private MainThread() {
   }

   public static void run(Plugin var0, Runnable var1) {
      if (var0 != null && var1 != null) {
         if (Bukkit.isPrimaryThread()) {
            var1.run();
         } else {
            Bukkit.getScheduler().runTask(var0, var1);
         }
      }
   }

   public static void runAsync(Plugin var0, Runnable var1) {
      if (var0 != null && var1 != null) {
         if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(var0, var1);
         } else {
            var1.run();
         }
      }
   }
}
