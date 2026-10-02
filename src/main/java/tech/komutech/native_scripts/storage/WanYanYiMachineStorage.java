package tech.komutech.native_scripts.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechPaths;

public final class WanYanYiMachineStorage {
   private WanYanYiMachineStorage() {
   }

   public static Path bindingDir() {
      Path var0 = KomutechPaths.configRoot().toPath().resolve("萬衍儀绑定");

      try {
         Files.createDirectories(var0);
      } catch (IOException var2) {
      }

      return var0;
   }

   public static Path bindingPath(Location var0) {
      return bindingDir().resolve(locationKey(var0) + ".json");
   }

   public static String locationKey(Location var0) {
      return var0.getWorld().getUID() + "_" + var0.getBlockX() + "_" + var0.getBlockY() + "_" + var0.getBlockZ();
   }

   public static void save(Location var0, ItemStack var1) {
      if (var0 != null && var0.getWorld() != null) {
         Path var2 = bindingPath(var0);
         if (var1 != null && !var1.getType().isAir() && WanXiangGuiStorage.isStorageItem(var1)) {
            LinkedHashMap var3 = new LinkedHashMap();
            var3.put("world", var0.getWorld().getUID().toString());
            var3.put("x", var0.getBlockX());
            var3.put("y", var0.getBlockY());
            var3.put("z", var0.getBlockZ());
            var3.put("item", Base64.getEncoder().encodeToString(var1.serializeAsBytes()));

            try {
               Files.writeString(var2, KomutechJson.stringify(var3), StandardCharsets.UTF_8);
            } catch (IOException var5) {
            }
         } else {
            clear(var0);
         }
      }
   }

   public static ItemStack load(Location var0) {
      if (var0 == null) {
         return null;
      } else {
         Path var1 = bindingPath(var0);
         if (!Files.exists(var1)) {
            return null;
         } else {
            try {
               Map var2 = KomutechJson.asMap(KomutechJson.parse(Files.readString(var1, StandardCharsets.UTF_8)));
               String var3 = KomutechJson.getString(var2, "item", "");
               return var3.isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(var3));
            } catch (RuntimeException | IOException var4) {
               return null;
            }
         }
      }
   }

   public static void clear(Location var0) {
      if (var0 != null) {
         try {
            Files.deleteIfExists(bindingPath(var0));
         } catch (IOException var2) {
         }
      }
   }
}
