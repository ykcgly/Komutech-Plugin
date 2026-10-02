package tech.komutech.native_scripts.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class CombatHitGate {
   private static final Map<String, Long> READY_AT_MS = new ConcurrentHashMap<>();

   private CombatHitGate() {
   }

   public static boolean tryAllow(Player var0, LivingEntity var1, int var2) {
      if (var0 != null && ScrollCombatHelper.isValidTarget(var0, var1)) {
         int var3 = Math.max(1, var2);
         String var4 = key(var0.getUniqueId(), var1.getUniqueId());
         long var5 = System.currentTimeMillis();
         Long var7 = READY_AT_MS.get(var4);
         if (var7 != null && var5 < var7) {
            return false;
         } else {
            READY_AT_MS.put(var4, var5 + var3 * 50L);
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean damage(Player var0, LivingEntity var1, double var2, int var4) {
      if (!tryAllow(var0, var1, var4)) {
         return false;
      } else {
         var1.damage(var2, var0);
         return true;
      }
   }

   private static String key(UUID var0, UUID var1) {
      return var0 + ":" + var1;
   }
}
