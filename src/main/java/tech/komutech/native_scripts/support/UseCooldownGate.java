package tech.komutech.native_scripts.support;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class UseCooldownGate {
   private static final Map<String, Long> LAST_USE_TICK = new ConcurrentHashMap<>();
   private static final Map<String, CooldownMap> SHARED_COOLDOWNS = new ConcurrentHashMap<>();

   private UseCooldownGate() {
   }

   public static CooldownMap sharedCooldown(String var0, long var1) {
      long var3 = Math.max(var1 * 2L, 60000L);
      return SHARED_COOLDOWNS.computeIfAbsent(var0, var4 -> new CooldownMap(var1, var3));
   }

   public static boolean tryUse(Player var0, String var1, CooldownMap var2, Runnable var3) {
      if (var0 != null && var1 != null && !var1.isBlank() && var2 != null) {
         String var4 = var0.getUniqueId() + "|" + var1;
         long var5 = var0.getWorld().getFullTime();
         Long var7 = LAST_USE_TICK.get(var4);
         if (var7 != null && var7 == var5) {
            if (var3 != null) {
               var3.run();
            }

            return false;
         } else if (!var2.tryAcquire(var0.getUniqueId())) {
            if (var3 != null) {
               var3.run();
            }

            return false;
         } else {
            LAST_USE_TICK.put(var4, var5);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void clearPlayer(UUID var0) {
      if (var0 != null) {
         String var1 = var0 + "|";
         LAST_USE_TICK.keySet().removeIf(var1x -> var1x.startsWith(var1));
      }
   }
}
