package tech.komutech.native_scripts;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class NativePluginHelper {
   private NativePluginHelper() {
   }

   public static Plugin getPlugin() {
      return Bukkit.getPluginManager().getPlugin("Komutech");
   }
}
