package tech.komutech.native_scripts.support;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class KomutechConfigMerge {
   private KomutechConfigMerge() {
   }

   public static Map<String, Object> loadMerged(Path var0, String var1) {
      Map var2 = readDisk(var0);
      Map var3 = readClasspath(var1);
      if (var3.isEmpty()) {
         return var2;
      } else {
         KomutechConfigMerge.MergeResult var4 = merge(var2, var3);
         if (var4.changed) {
            try {
               Path var5 = var0.getParent();
               if (var5 != null) {
                  Files.createDirectories(var5);
               }

               Files.writeString(var0, KomutechJson.stringify(var4.map), StandardCharsets.UTF_8);
            } catch (Exception var6) {
            }
         }

         return var4.map;
      }
   }

   private static KomutechConfigMerge.MergeResult merge(Map<String, Object> var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap(var0);
      boolean var3 = false;

      for (Entry var5 : var1.entrySet()) {
         String var6 = (String)var5.getKey();
         Object var7 = var5.getValue();
         Object var8 = var2.get(var6);
         if (var8 == null) {
            var2.put(var6, var7);
            var3 = true;
         } else if (var7 instanceof Map var9 && var8 instanceof Map) {
            Map var10 = KomutechJson.asMap(var8);
            Map var11 = KomutechJson.asMap(var9);
            KomutechConfigMerge.MergeResult var12 = merge(var10, var11);
            var2.put(var6, var12.map);
            var3 = var3 || var12.changed;
         } else if (!Objects.equals(String.valueOf(var8), String.valueOf(var7))) {
            var2.put(var6, var7);
            var3 = true;
         }
      }

      return new KomutechConfigMerge.MergeResult(var2, var3);
   }

   private static Map<String, Object> readDisk(Path var0) {
      try {
         if (var0 != null && Files.exists(var0)) {
            return KomutechJson.asMap(KomutechJson.parse(Files.readString(var0, StandardCharsets.UTF_8)));
         }
      } catch (Exception var2) {
      }

      return new LinkedHashMap<>();
   }

   private static Map<String, Object> readClasspath(String var0) {
      Plugin var1 = Bukkit.getPluginManager().getPlugin("Komutech");
      if (var1 != null && var0 != null && !var0.isBlank()) {
         try {
            Map var4;
            try (InputStream var2 = var1.getResource(var0)) {
               if (var2 == null) {
                  return new LinkedHashMap<>();
               }

               String var3 = new String(var2.readAllBytes(), StandardCharsets.UTF_8);
               var4 = KomutechJson.asMap(KomutechJson.parse(var3));
            }

            return var4;
         } catch (Exception var7) {
            return new LinkedHashMap<>();
         }
      } else {
         return new LinkedHashMap<>();
      }
   }

   private record MergeResult(Map<String, Object> map, boolean changed) {
   }
}
