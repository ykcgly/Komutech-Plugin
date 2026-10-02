package tech.komutech.native_scripts.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.EntityQueries;

public final class ScrollCombatHelper {
   private ScrollCombatHelper() {
   }

   public static boolean isValidTarget(Player var0, LivingEntity var1) {
      return var1 != null && var1 != var0 && !var1.getType().name().equals("ARMOR_STAND") && !var1.isDead() && var1.isValid()
         ? PvpGuard.canHarm(var0, var1)
         : false;
   }

   public static void damageTarget(Player var0, Map<String, Object> var1, LivingEntity var2, double var3) {
      if (isValidTarget(var0, var2)) {
         var2.damage(var3, var0);
         if (!(var2 instanceof Player)) {
            ScrollCombatEngine.meritChangeDelta(var1, var2.getType().name());
         }
      }
   }

   public static void damageAlongRay(Player var0, Map<String, Object> var1, Location var2, Vector var3, double var4, double var6, double var8) {
      HashSet var10 = new HashSet();
      int var11 = Math.max(1, (int)Math.ceil(var4 / Math.max(0.5, var6)));
      double var12 = var4 / var11;

      for (int var14 = 0; var14 <= var11; var14++) {
         Location var15 = var2.clone().add(var3.clone().multiply(var12 * var14));
         collectAndDamage(var0, var1, var10, var15, var6, var8);
      }
   }

   public static void damageInRadius(Player var0, Map<String, Object> var1, Location var2, double var3, double var5) {
      HashSet var7 = new HashSet();
      collectAndDamage(var0, var1, var7, var2, var3, var5);
   }

   private static void collectAndDamage(Player var0, Map<String, Object> var1, Set<LivingEntity> var2, Location var3, double var4, double var6) {
      for (LivingEntity var10 : EntityQueries.living(var3, var4)) {
         if (var2.add(var10)) {
            damageTarget(var0, var1, var10, var6);
         }
      }
   }

   public static void applyBatchMerit(Map<String, Object> var0, int var1, int var2, int var3, int var4) {
      int var5 = 0;
      ThreadLocalRandom var6 = ThreadLocalRandom.current();
      if (var1 > 0) {
         var5 -= var1 * var6.nextInt(var3, var4 + 1);
      }

      if (var2 > 0) {
         var5 += var2 * var6.nextInt(var3, var4 + 1);
      }

      if (var5 != 0) {
         int var7 = ScrollCombatEngine.getIntAttr(var0, "功德", 0) + var5;
         var0.put("功德", Math.max(0, var7));
         if (var5 < 0) {
            var0.put("煞气", ScrollCombatEngine.getIntAttr(var0, "煞气", 0) + Math.abs(var5));
         }
      }
   }

   public static int recordMeritHit(LivingEntity var0, List<String> var1, ScrollCombatHelper.MeritTracker var2) {
      if (var0 instanceof Player) {
         return 0;
      } else {
         if (var1.contains(var0.getType().name())) {
            var2.whiteHits++;
         } else {
            var2.normalHits++;
         }

         return 1;
      }
   }

   public static final class MeritTracker {
      int whiteHits;
      int normalHits;
   }
}
