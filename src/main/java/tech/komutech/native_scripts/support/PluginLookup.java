package tech.komutech.native_scripts.support;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginLookup {
   private PluginLookup() {
   }

   public static Plugin metadataPlugin(String var0) {
      return firstAvailable(var0, "RykenSlimefunCustomizer");
   }

   public static Plugin schedulerPlugin(String var0) {
      Plugin var1 = Bukkit.getPluginManager().getPlugin("Slimefun");
      if (var1 == null) {
         var1 = metadataPlugin(var0);
      }

      return var1;
   }

   public static FileConfiguration addonConfig(String var0) {
      return Bukkit.getPluginManager().getPlugin(var0) instanceof JavaPlugin var2 ? var2.getConfig() : null;
   }

   private static Plugin firstAvailable(String... var0) {
      for (String var4 : var0) {
         Plugin var5 = Bukkit.getPluginManager().getPlugin(var4);
         if (var5 != null) {
            return var5;
         }
      }

      return null;
   }
}
