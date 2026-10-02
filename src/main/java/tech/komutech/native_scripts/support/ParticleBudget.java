package tech.komutech.native_scripts.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.util.Vector;

public final class ParticleBudget {
   private static final int MAX_PER_WORLD_TICK = 480;
   private static final Map<String, Long> worldTicks = new ConcurrentHashMap<>();
   private static final Map<String, Integer> worldUsage = new ConcurrentHashMap<>();

   private ParticleBudget() {
   }

   public static int request(World var0, int var1) {
      if (var0 != null && var1 > 0) {
         String var2 = var0.getName();
         long var3 = var0.getFullTime();
         worldTicks.compute(var2, (var2x, var3x) -> {
            if (var3x == null || var3x != var3) {
               worldUsage.put(var2x, 0);
            }

            return var3;
         });
         int var5 = worldUsage.getOrDefault(var2, 0);
         int var6 = Math.min(var1, Math.max(0, 480 - var5));
         if (var6 > 0) {
            worldUsage.put(var2, var5 + var6);
         }

         return var6;
      } else {
         return 0;
      }
   }

   public static boolean canRunHeavyEffect(World var0) {
      return request(var0, 1) > 0;
   }

   public static int scaleTrailPoints(int var0) {
      return var0 >= 12 ? 8 : var0;
   }

   public static void spawnDust(World var0, Location var1, DustOptions var2, int var3) {
      if (var0 != null && var1 != null && var2 != null && var3 > 0) {
         int var4 = request(var0, var3);
         if (var4 > 0) {
            var0.spawnParticle(Particle.DUST, var1, var4, 0.0, 0.0, 0.0, 0.0, var2);
         }
      }
   }

   public static void spawnEndRod(World var0, Location var1, double var2, double var4, double var6, double var8, int var10) {
      if (var0 != null && var1 != null && var10 > 0) {
         int var11 = request(var0, var10);
         if (var11 > 0) {
            var0.spawnParticle(Particle.END_ROD, var1, var11, var2, var4, var6, var8);
         }
      }
   }

   public static void spawnRayTrail(World var0, Location var1, Vector var2, double var3, Color var5, Color var6, float var7) {
      spawnRayTrail(var0, var1, var2, var3, var5, var6, var7, 0.25);
   }

   public static void spawnRayTrail(World var0, Location var1, Vector var2, double var3, Color var5, Color var6, float var7, double var8) {
      if (var0 != null && var1 != null && var2 != null && !(var3 <= 0.0)) {
         Vector var10 = var2.clone();
         if (!(var10.lengthSquared() < 1.0E-4)) {
            var10.normalize();
            double var11 = var8 > 0.0 ? var8 : 0.25;
            float var13 = Math.max(1.0F, Math.min(4.0F, var7));

            for (double var14 = 0.0; var14 <= var3; var14 += var11) {
               double var16 = var14 / var3;
               int var18 = (int)Math.round(var5.getRed() + (var6.getRed() - var5.getRed()) * var16);
               int var19 = (int)Math.round(var5.getGreen() + (var6.getGreen() - var5.getGreen()) * var16);
               int var20 = (int)Math.round(var5.getBlue() + (var6.getBlue() - var5.getBlue()) * var16);
               DustOptions var21 = new DustOptions(Color.fromRGB(var18, var19, var20), var13);
               Location var22 = var1.clone().add(var10.clone().multiply(var14));
               var0.spawnParticle(Particle.DUST, var22, 1, 0.0, 0.0, 0.0, 0.0, var21);
            }
         }
      }
   }
}
