package tech.komutech.native_scripts.support;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class KomutechAddonConfig {
   private static File configFile;
   private static YamlConfiguration config;
   private static long lastModified;

   private KomutechAddonConfig() {
   }

   private static YamlConfiguration yaml() {
      File var0 = new File(KomutechPaths.configRoot(), "config.yml");
      long var1 = var0.exists() ? var0.lastModified() : 0L;
      if (config == null || configFile == null || var1 != lastModified || !var0.equals(configFile)) {
         configFile = var0;
         config = YamlConfiguration.loadConfiguration(var0);
         lastModified = var1;
      }

      return config;
   }

   public static int getInt(String var0, int var1) {
      return yaml().getInt(var0, var1);
   }

   public static String getString(String var0, String var1) {
      String var2 = yaml().getString(var0, var1);
      return var2 == null ? var1 : var2;
   }

   public static List<String> getStringList(String var0) {
      List var1 = yaml().getStringList(var0);
      return var1 == null ? Collections.emptyList() : var1;
   }
}
