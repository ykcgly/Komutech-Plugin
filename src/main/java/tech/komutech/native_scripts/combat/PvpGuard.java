package tech.komutech.native_scripts.combat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechPaths;

public final class PvpGuard {
   private static final long CACHE_MS = 5000L;
   private static volatile Set<String> disabledCache = Set.of();
   private static volatile long cachedAt;

   private PvpGuard() {
   }

   public static boolean isSpiritPvpEnabled(String var0) {
      return var0 != null && !var0.isBlank() ? !disabledNames().contains(var0) : false;
   }

   public static boolean canHarm(Player var0, LivingEntity var1) {
      if (var0 == null || var1 == null || var1 == var0) {
         return false;
      } else if (var1 instanceof Player var2) {
         if (!var0.getWorld().getPVP()) {
            return false;
         } else {
            return isSpiritPvpEnabled(var0.getName()) && isSpiritPvpEnabled(var2.getName()) ? !isProtectedByOtherPlugins(var0, var2) : false;
         }
      } else {
         return true;
      }
   }

   private static boolean isProtectedByOtherPlugins(Player var0, Player var1) {
      try {
         EntityDamageByEntityEvent var2 = new EntityDamageByEntityEvent(var0, var1, DamageCause.ENTITY_ATTACK, 0.01);
         Bukkit.getPluginManager().callEvent(var2);
         return var2.isCancelled();
      } catch (Throwable var3) {
         return false;
      }
   }

   private static Set<String> disabledNames() {
      long var0 = System.currentTimeMillis();
      if (var0 - cachedAt < 5000L) {
         return disabledCache;
      } else {
         Set var2 = loadDisabled();
         disabledCache = var2;
         cachedAt = var0;
         return var2;
      }
   }

   private static Set<String> loadDisabled() {
      Path var0 = KomutechPaths.configRoot().toPath().resolve("灵PVP列表.json");

      try {
         if (!Files.exists(var0)) {
            return Set.of();
         } else {
            Object var1 = KomutechJson.parse(Files.readString(var0, StandardCharsets.UTF_8));
            HashSet var2 = new HashSet();
            if (var1 instanceof List) {
               for (Object var6 : (List)var1) {
                  if (var6 != null) {
                     var2.add(String.valueOf(var6));
                  }
               }
            } else if (var1 instanceof Map var4) {
               for (Object var9 : var4.keySet()) {
                  if (var9 != null) {
                     var2.add(String.valueOf(var9));
                  }
               }
            }

            return Collections.unmodifiableSet(var2);
         }
      } catch (Exception var7) {
         return Set.of();
      }
   }
}
