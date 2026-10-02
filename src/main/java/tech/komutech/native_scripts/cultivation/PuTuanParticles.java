package tech.komutech.native_scripts.cultivation;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.ParticleBudget;

final class PuTuanParticles {
   private static final Color CYAN = Color.fromRGB(120, 220, 255);
   private static final Color PURPLE = Color.fromRGB(180, 90, 255);
   private static final Color GOLD = Color.fromRGB(255, 215, 0);
   private static final Color WHITE = Color.fromRGB(255, 255, 220);
   private static final Color RED = Color.fromRGB(200, 40, 40);
   private static final Color ELECTRIC = Color.fromRGB(180, 220, 255);

   private PuTuanParticles() {
   }

   static void meditation(Player var0) {
      Location var1 = var0.getLocation();
      int var2 = ParticleBudget.request(var0.getWorld(), 3);
      if (var2 > 0) {
         ThreadLocalRandom var3 = ThreadLocalRandom.current();
         int var4 = Math.min(var2, 2 + var3.nextInt(2));
         spawnRing(var0, Particle.END_ROD, var4, var1, 0.55, 0.35, 0.45, 0.9, null);
         if (var4 < var2) {
            spawnDust(var0, var1.clone().add(0.0, 0.75, 0.0), CYAN, 1.0F);
         }
      }
   }

   static void breakthroughProgress(Player var0) {
      Location var1 = var0.getLocation();
      int var2 = ParticleBudget.request(var0.getWorld(), 8);
      if (var2 > 0) {
         ThreadLocalRandom var3 = ThreadLocalRandom.current();
         int var4 = Math.min(var2 - 1, 4 + var3.nextInt(3));
         spawnRing(var0, Particle.ENCHANT, var4, var1, 0.5, 0.55, 0.35, 1.1, null);
         spawnDust(var0, var1.clone().add(0.0, 1.0, 0.0), PURPLE, 1.2F);
         if (var2 >= 7 && var3.nextBoolean()) {
            spawnDust(var0, var1.clone().add(0.0, 0.5, 0.0), Color.fromRGB(255, 120, 200), 0.9F);
         }
      }
   }

   static void breakthroughSuccess(Player var0) {
      var0.playSound(var0.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);
      scheduleBurst(var0, 3, 4L, var1 -> {
         Location var2 = var0.getLocation().add(0.0, 0.9 + var1 * 0.15, 0.0);
         int var3 = ParticleBudget.request(var0.getWorld(), 10);
         if (var3 > 0) {
            ThreadLocalRandom var4 = ThreadLocalRandom.current();
            int var5 = Math.min(var3, 6 + var1);

            for (int var6 = 0; var6 < var5; var6++) {
               double var7 = var4.nextDouble(Math.PI * 2);
               double var9 = 0.4 + var4.nextDouble(1.0 + var1 * 0.2);
               Location var11 = var2.clone().add(Math.cos(var7) * var9, var4.nextDouble(1.2), Math.sin(var7) * var9);
               Color var12 = var6 % 2 == 0 ? GOLD : WHITE;
               spawnDust(var0, var11, var12, 1.4F);
            }

            if (var1 == 2 && ParticleBudget.request(var0.getWorld(), 2) > 0) {
               var0.spawnParticle(Particle.TOTEM_OF_UNDYING, var2, 3, 0.25, 0.4, 0.25, 0.02);
            }
         }
      });
   }

   static void breakthroughFailure(Player var0) {
      var0.playSound(var0.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7F, 0.7F);
      scheduleBurst(var0, 2, 5L, var1 -> {
         Location var2 = var0.getLocation().add(0.0, 0.7, 0.0);
         int var3 = ParticleBudget.request(var0.getWorld(), 10);
         if (var3 > 0) {
            ThreadLocalRandom var4 = ThreadLocalRandom.current();
            int var5 = Math.min(var3 - 1, 5 + var1 * 2);

            for (int var6 = 0; var6 < var5; var6++) {
               double var7 = var4.nextDouble(Math.PI * 2);
               double var9 = 0.35 + var4.nextDouble(0.9);
               Location var11 = var2.clone().add(Math.cos(var7) * var9, var4.nextDouble(0.8), Math.sin(var7) * var9);
               spawnDust(var0, var11, RED, 1.1F);
            }

            if (ParticleBudget.request(var0.getWorld(), 1) > 0) {
               var0.spawnParticle(Particle.SMOKE, var2, 2, 0.2, 0.25, 0.2, 0.01);
            }
         }
      });
   }

   static void tribulationStrike(Player var0) {
      Location var1 = var0.getLocation();
      var0.playSound(var1, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.45F, 1.1F);
      var0.playSound(var1, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.35F, 1.3F);
      int var2 = ParticleBudget.request(var0.getWorld(), 10);
      if (var2 > 0) {
         ThreadLocalRandom var3 = ThreadLocalRandom.current();
         if (ParticleBudget.request(var0.getWorld(), 1) > 0) {
            var0.spawnParticle(Particle.FLASH, var1.clone().add(0.0, 1.2, 0.0), 1, 0.0, 0.0, 0.0, 0.0, WHITE);
         }

         int var4 = Math.min(var2, 4 + var3.nextInt(3));

         for (int var5 = 0; var5 < var4; var5++) {
            Location var6 = var1.clone().add((var3.nextDouble() - 0.5) * 0.6, 2.5 + var3.nextDouble(1.5), (var3.nextDouble() - 0.5) * 0.6);
            Location var7 = var1.clone().add((var3.nextDouble() - 0.5) * 0.8, 0.3 + var3.nextDouble(0.8), (var3.nextDouble() - 0.5) * 0.8);
            var0.spawnParticle(Particle.ELECTRIC_SPARK, var6, 1, 0.0, -0.15, 0.0, 0.02);
            var0.spawnParticle(Particle.ELECTRIC_SPARK, var7, 1, 0.04, 0.04, 0.04, 0.01);
            spawnDust(var0, var7, ELECTRIC, 0.9F);
         }
      }
   }

   private static void spawnRing(Player var0, Particle var1, int var2, Location var3, double var4, double var6, double var8, double var10, DustOptions var12) {
      ThreadLocalRandom var13 = ThreadLocalRandom.current();

      for (int var14 = 0; var14 < var2; var14++) {
         double var15 = var13.nextDouble(Math.PI * 2);
         double var17 = var4 + var13.nextDouble(var6);
         Location var19 = var3.clone().add(var17 * Math.cos(var15), var8 + var13.nextDouble(var10), var17 * Math.sin(var15));
         if (var12 != null) {
            var0.spawnParticle(Particle.DUST, var19, 1, 0.0, 0.0, 0.0, 0.0, var12);
         } else {
            var0.spawnParticle(var1, var19, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void spawnDust(Player var0, Location var1, Color var2, float var3) {
      if (ParticleBudget.request(var0.getWorld(), 1) > 0) {
         var0.spawnParticle(Particle.DUST, var1, 1, 0.03, 0.05, 0.03, 0.0, new DustOptions(var2, var3));
      }
   }

   private static void scheduleBurst(Player var0, int var1, long var2, PuTuanParticles.BurstWave var4) {
      Plugin var5 = KomutechSupport.plugin();
      if (var5 == null) {
         var4.run(0);
      } else {
         for (int var6 = 0; var6 < var1; var6++) {
            int var7 = var6;
            Bukkit.getScheduler().runTaskLater(var5, () -> {
               if (var0.isOnline()) {
                  var4.run(var7);
               }
            }, var2 * var6);
         }
      }
   }

   @FunctionalInterface
   private interface BurstWave {
      void run(int var1);
   }
}
