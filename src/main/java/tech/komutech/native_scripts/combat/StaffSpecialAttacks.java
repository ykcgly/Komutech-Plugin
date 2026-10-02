package tech.komutech.native_scripts.combat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.ParticleBudget;

public final class StaffSpecialAttacks {
   private static final double FYM_PROJECTILE_SPEED = 1.6;
   private static final double FYM_SPLIT_RATIO = 0.5;
   private static final double FYM_SUB_SPEED = 1.2;
   private static final double FYM_SUB_ANGLE = 0.26;
   private static final int FYM_RESIDUE_RADIUS_TICKS = 30;
   private static final double FYM_RESIDUE_RADIUS = 2.5;
   private static final double FYM_RESIDUE_DAMAGE_RATIO = 0.2;
   private static final int FYM_BLIND_DURATION = 20;
   private static final double KYJ_SPHERE_RADIUS = 1.5;
   private static final int KYJ_SPHERE_STEPS = 12;
   private static final int KYJ_SPHERE_INTERVAL = 1;
   private static final int KYJ_AREA_DAMAGE_INTERVAL = 2;
   private static final double KYJ_CIRCLE_RADIUS = 6.0;
   private static final double KYJ_CIRCLE_HEIGHT = 1.0;
   private static final int KYJ_CIRCLE_DAMAGE_INTERVAL = 10;
   private static final int GX_CHARGE_TICKS = 8;
   private static final double GX_BURST_RADIUS = 5.5;
   private static final double GX_PULL_STRENGTH = 0.22;
   private static final int GX_RING_POINTS = 10;
   private static final Color GX_VOID = Color.fromRGB(72, 32, 120);
   private static final Color GX_STAR = Color.fromRGB(230, 220, 255);
   private static final Color GX_CORE = Color.fromRGB(180, 140, 255);

   private StaffSpecialAttacks() {
   }

   public static void guiXu(Player var0, ItemStack var1, Map<String, Object> var2) {
      double var3 = ScrollCombatEngine.staffMaxDistance("归墟");
      double var5 = ScrollCombatEngine.calcStaffDamage(var0, var1, var2);
      World var7 = var0.getWorld();
      Location var8 = var0.getEyeLocation();
      Vector var9 = var8.getDirection();
      if (var9.lengthSquared() < 1.0E-6) {
         var9 = var8.getDirection().setY(0);
         if (var9.lengthSquared() < 1.0E-6) {
            var9 = new Vector(0, 0, 1);
         }
      }

      var9.normalize();
      Location var10 = var8.clone().add(var9);
      RayTraceResult var11 = var7.rayTrace(
         var10,
         var9,
         var3,
         FluidCollisionMode.NEVER,
         true,
         0.35,
         var1x -> var1x != var0 && var1x instanceof LivingEntity && !var1x.getType().name().equals("ARMOR_STAND") && !var1x.isDead()
      );
      Location var12;
      if (var11 != null && var11.getHitPosition() != null) {
         var12 = var11.getHitPosition().toLocation(var7).add(0.0, 0.35, 0.0);
      } else {
         var12 = var10.clone().add(var9.clone().multiply(var3));
      }

      spawnGuiXuAimBeam(var7, var10, var12);
      var7.playSound(var12, "block.respawn_anchor.charge", 0.55F, 1.35F);
      Plugin var13 = plugin();
      if (var13 == null) {
         collapseGuiXu(var7, var12, var0, var2, var5);
      } else {
         int[] var14 = new int[]{0};
         Bukkit.getScheduler().runTaskTimer(var13, var7x -> {
            if (!var0.isOnline() || var0.isDead() || var0.getWorld() != var7) {
               var7x.cancel();
            } else if (var14[0] < 8) {
               spawnGuiXuChargeRing(var7, var12, var14[0]);
               var14[0]++;
            } else {
               var7x.cancel();
               collapseGuiXu(var7, var12, var0, var2, var5);
            }
         }, 0L, 1L);
      }
   }

   private static void spawnGuiXuAimBeam(World var0, Location var1, Location var2) {
      Vector var3 = var2.toVector().subtract(var1.toVector());
      double var4 = var3.length();
      if (!(var4 < 0.2)) {
         Vector var6 = var3.normalize().multiply(0.55);
         Location var7 = var1.clone();
         int var8 = Math.min(48, (int)Math.ceil(var4 / 0.55));
         DustOptions var9 = new DustOptions(GX_CORE, 0.9F);

         for (int var10 = 0; var10 < var8; var10++) {
            var7.add(var6);
            if (ParticleBudget.request(var0, 1) <= 0) {
               break;
            }

            var0.spawnParticle(Particle.DUST, var7, 1, 0.0, 0.0, 0.0, 0.0, var9);
            if (var10 % 3 == 0) {
               ParticleBudget.spawnEndRod(var0, var7, 0.0, 0.0, 0.0, 0.0, 1);
            }
         }
      }
   }

   private static void spawnGuiXuChargeRing(World var0, Location var1, int var2) {
      double var3 = (double)var2 / Math.max(1, 7);
      double var5 = 5.5 * (1.0 - var3 * 0.75);
      int var7 = ParticleBudget.request(var0, 10);
      if (var7 > 0) {
         DustOptions var8 = new DustOptions(GX_VOID, 1.15F);
         DustOptions var9 = new DustOptions(GX_STAR, 0.85F);

         for (int var10 = 0; var10 < var7; var10++) {
            double var11 = (Math.PI * 2) * var10 / var7 + var2 * 0.18;
            double var13 = var1.getX() + Math.cos(var11) * var5;
            double var15 = var1.getZ() + Math.sin(var11) * var5;
            double var17 = var1.getY() + Math.sin(var2 * 0.4 + var10) * 0.15;
            var0.spawnParticle(Particle.DUST, var13, var17, var15, 1, 0.0, 0.0, 0.0, 0.0, var8);
            if (var10 % 2 == 0) {
               var0.spawnParticle(Particle.DUST, var13, var17 + 0.25, var15, 1, 0.0, 0.0, 0.0, 0.0, var9);
            }
         }

         if (var2 % 2 == 0) {
            ParticleBudget.spawnEndRod(var0, var1, 0.12, 0.2, 0.12, 0.01, 1);
         }
      }
   }

   private static void collapseGuiXu(World var0, Location var1, Player var2, Map<String, Object> var3, double var4) {
      HashSet var6 = new HashSet();
      int var7 = 0;
      int var8 = 0;

      for (LivingEntity var10 : EntityQueries.living(var0, var1, 5.5)) {
         if (var10 != var2 && !var10.getType().name().equals("ARMOR_STAND") && !var10.isDead() && var6.add(var10.getUniqueId())) {
            var8++;
            var10.damage(var4, var2);
            if (!(var10 instanceof Player)) {
               var7 += applyMeritChange(var3, var10.getType().name());
            }
         }
      }

      safePull(var0, var1, var2, 0.22, 5.5);
      spawnGuiXuCollapseBurst(var0, var1);
      var0.playSound(var1, "block.respawn_anchor.deplete", 0.8F, 0.7F);
      var0.playSound(var1, "entity.warden.sonic_boom", 0.35F, 1.6F);
      if (var8 > 0) {
         KomutechSupport.send(var2, "§5归墟坍缩 §7· §f命中 §d" + var8 + " §f目标");
      } else {
         KomutechSupport.send(var2, "§5归墟坍缩 §7· §8虚空无物");
      }

      if (var7 != 0) {
         KomutechSupport.send(var2, var7 > 0 ? "§2✦ §a[归墟] 积德行善！§6+" + var7 + "功德" : "§4✧ §c[归墟] 损德败行！§6" + var7 + "功德");
      }
   }

   private static void spawnGuiXuCollapseBurst(World var0, Location var1) {
      int var2 = ParticleBudget.request(var0, 16);
      DustOptions var3 = new DustOptions(GX_CORE, 1.4F);
      DustOptions var4 = new DustOptions(GX_STAR, 1.1F);

      for (int var5 = 0; var5 < var2; var5++) {
         double var6 = (Math.PI * 2) * var5 / Math.max(1, var2);
         double var8 = 4.675;
         Location var10 = new Location(var0, var1.getX() + Math.cos(var6) * var8, var1.getY() + 0.2, var1.getZ() + Math.sin(var6) * var8);
         var0.spawnParticle(Particle.DUST, var10, 1, 0.0, 0.0, 0.0, 0.0, var3);
         if (var5 % 2 == 0) {
            var0.spawnParticle(Particle.DUST, var10.clone().add(0.0, 0.45, 0.0), 1, 0.0, 0.0, 0.0, 0.0, var4);
         }
      }

      ParticleBudget.spawnEndRod(var0, var1, 0.35, 0.55, 0.35, 0.04, 8);

      for (int var11 = 0; var11 < 4; var11++) {
         Location var12 = var1.clone().add(0.0, var11 * 0.55, 0.0);
         ParticleBudget.spawnDust(var0, var12, new DustOptions(GX_VOID, 1.2F), 1);
      }
   }

   public static void fuyouMeng(Player var0, ItemStack var1, Map<String, Object> var2) {
      double var3 = ScrollCombatEngine.staffMaxDistance("蜉蝣梦");
      double var5 = ScrollCombatEngine.calcStaffDamage(var0, var1, var2);
      Color var7 = Color.fromRGB(176, 224, 230);
      Color var8 = Color.fromRGB(255, 255, 255);
      launchMainProjectile(var0, var2, var5, var3, var7, var8);
   }

   public static void kongYinJin(Player var0, ItemStack var1, Map<String, Object> var2) {
      double var3 = ScrollCombatEngine.staffMaxDistance("空引津");
      double var5 = ScrollCombatEngine.calcStaffDamage(var0, var1, var2);
      World var7 = var0.getWorld();
      Location var8 = var0.getEyeLocation();
      Vector var9 = var8.getDirection();
      Location var10 = var8.clone().add(var9);
      double var11 = var3 / 12.0;
      Plugin var13 = plugin();
      if (var13 != null) {
         int[] var14 = new int[]{0};
         int[] var15 = new int[]{0};
         int[] var16 = new int[]{0};
         boolean[] var17 = new boolean[]{false};
         int var18 = Math.max(1, 10);
         Bukkit.getScheduler().runTaskTimer(var13, var14x -> {
            if (var0.isOnline() && !var0.isDead() && var0.getWorld() == var7 && var14[0] < 12) {
               Location var20 = var10.clone().add(var9.clone().multiply(var11 * var14[0]));
               spawnSphereParticles(var7, var20);
               spawnCircleParticles(var7, var0.getLocation());
               if (var14[0] % 2 == 0) {
                  Collection<LivingEntity> var16x = EntityQueries.living(var7, var20, 1.5);
                  LivingEntity var17x = null;

                  for (LivingEntity var19 : var16x) {
                     if (var19 != var0 && !var19.getType().name().equals("ARMOR_STAND") && !var19.isDead()) {
                        if (var17x == null) {
                           var17x = var19;
                        }

                        var16[0] += processEntityDamage(var19, var0, var5, var2);
                     }
                  }

                  if (var17x != null && !var17[0]) {
                     var17[0] = true;
                     var16[0] += noteExplosion(var7, var17x.getLocation(), var0, var2, var5, 0.6);
                  }
               }

               var15[0]++;
               if (var15[0] >= var18) {
                  var15[0] = 0;
                  var16[0] += applyAreaDamage(var7, var0.getLocation(), var0, var5, 6.0, 1.0, 6.0, var2);
               }

               var14[0]++;
            } else {
               var14x.cancel();
               if (!var17[0]) {
                  Location var15x = var10.clone().add(var9.clone().multiply(var11 * var14[0]));
                  var16[0] += noteExplosion(var7, var15x, var0, var2, var5, 0.5);
               }

               if (var16[0] != 0) {
                  KomutechSupport.send(var0, var16[0] > 0 ? "§2✦ §a[空引津] 积德行善！§6+" + var16[0] + "功德" : "§4✧ §c[空引津] 损德败行！§6" + var16[0] + "功德");
               }
            }
         }, 0L, 1L);
      }
   }

   private static void launchMainProjectile(Player var0, Map<String, Object> var1, double var2, double var4, Color var6, Color var7) {
      World var8 = var0.getWorld();
      Location var9 = var0.getEyeLocation();
      Vector var10 = var9.getDirection();
      Location var11 = var9.clone().add(var10);
      Plugin var12 = plugin();
      if (var12 != null) {
         double[] var13 = new double[]{0.0};
         boolean[] var14 = new boolean[]{false};
         int[] var15 = new int[]{0};
         Bukkit.getScheduler()
            .runTaskTimer(
               var12,
               var14x -> {
                  if (!(var13[0] >= var4) && var0.isOnline() && !var0.isDead() && var0.getWorld() == var8) {
                     var13[0]++;
                     Location var19 = var11.clone().add(var10.clone().multiply(var13[0]));
                     spawnProjectileTrail(var8, var19, var6, var7);
                     var15[0]++;
                     if (var15[0] % 3 == 0) {
                        ParticleBudget.spawnDust(var8, var19, new DustOptions(var6, 1.2F), 1);
                     }

                     if (!var14[0] && var13[0] >= var4 * 0.5) {
                        var14[0] = true;
                        Vector var16 = var10.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                        launchSubProjectile(var8, var19, var10.clone().add(var16.clone().multiply(0.26)).normalize(), var0, var1, var2, var4, var6);
                        launchSubProjectile(var8, var19, var10.clone().add(var16.clone().multiply(-0.26)).normalize(), var0, var1, var2, var4, var6);
                        launchSubProjectile(var8, var19, var10.clone(), var0, var1, var2, var4, var6);
                     }

                     for (LivingEntity var17 : EntityQueries.living(var8, var19, 1.0)) {
                        if (var17 != var0 && !var17.getType().name().equals("ARMOR_STAND") && !var17.isDead()) {
                           var14x.cancel();
                           hitBurst(var8, var19, var6);
                           int var18 = damageEntity(var17, var0, var2, var1);
                           if (!(var17 instanceof Player)) {
                              KomutechSupport.send(
                                 var0,
                                 var18 < 0
                                    ? "§c攻击§6" + var17.getType().name() + "§c，煞气+" + Math.abs(var18) + "§c！"
                                    : "§a攻击§6" + var17.getType().name() + "§a，功德+" + var18 + "§a！"
                              );
                           }

                           spawnResidue(var8, var19, var0, var1, var2);
                           return;
                        }
                     }
                  } else {
                     var14x.cancel();
                     if (var13[0] >= var4) {
                        Location var15x = var11.clone().add(var10.clone().multiply(var13[0]));
                        hitBurst(var8, var15x, var6);
                        spawnResidue(var8, var15x, var0, var1, var2);
                     }
                  }
               },
               0L,
               1L
            );
      }
   }

   private static void launchSubProjectile(World var0, Location var1, Vector var2, Player var3, Map<String, Object> var4, double var5, double var7, Color var9) {
      Plugin var10 = plugin();
      if (var10 != null) {
         double[] var11 = new double[]{0.0};
         Bukkit.getScheduler().runTaskTimer(var10, var11x -> {
            if (!(var11[0] >= var7 * 0.7) && var3.isOnline() && var3.getWorld() == var0) {
               var11[0]++;
               Location var12 = var1.clone().add(var2.clone().multiply(var11[0]));
               ParticleBudget.spawnDust(var0, var12, new DustOptions(var9, 0.8F), 3);

               for (LivingEntity var14 : EntityQueries.living(var0, var12, 0.8)) {
                  if (var14 != var3 && !var14.getType().name().equals("ARMOR_STAND") && !var14.isDead()) {
                     damageEntity(var14, var3, var5 * 0.6, var4);
                     var11x.cancel();
                     hitBurst(var0, var12, var9);
                     return;
                  }
               }
            } else {
               var11x.cancel();
            }
         }, 0L, 1L);
      }
   }

   private static void spawnProjectileTrail(World var0, Location var1, Color var2, Color var3) {
      ThreadLocalRandom var4 = ThreadLocalRandom.current();

      for (int var5 = 0; var5 < 3; var5++) {
         double var6 = (var4.nextDouble() - 0.5) * 0.5;
         Location var8 = var1.clone().add(var6, var6, var6);
         var0.spawnParticle(Particle.DUST, var8, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var2, 1.3F));
      }

      var0.spawnParticle(Particle.DUST, var1, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var3, 0.7F));
      var0.spawnParticle(Particle.END_ROD, var1, 1, 0.02, 0.02, 0.02, 0.01);
   }

   private static void hitBurst(World var0, Location var1, Color var2) {
      ThreadLocalRandom var3 = ThreadLocalRandom.current();
      int var4 = ParticleBudget.request(var0, 12);

      for (int var5 = 0; var5 < var4; var5++) {
         double var6 = (var3.nextDouble() - 0.5) * 2.0;
         double var8 = (var3.nextDouble() - 0.5) * 2.0;
         double var10 = (var3.nextDouble() - 0.5) * 2.0;
         var0.spawnParticle(Particle.DUST, var1.getX() + var6, var1.getY() + var8, var1.getZ() + var10, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var2, 1.5F));
      }
   }

   private static void spawnResidue(World var0, Location var1, Player var2, Map<String, Object> var3, double var4) {
      Color var6 = Color.fromRGB(230, 230, 250);
      Plugin var7 = plugin();
      if (var7 != null) {
         int[] var8 = new int[]{0};
         Bukkit.getScheduler().runTaskTimer(var7, var8x -> {
            if (var8[0] >= 30) {
               var8x.cancel();
            } else {
               ThreadLocalRandom var9 = ThreadLocalRandom.current();
               int var10 = ParticleBudget.request(var0, 4);

               for (int var11 = 0; var11 < var10; var11++) {
                  double var12 = var9.nextDouble(Math.PI * 2);
                  double var14 = var9.nextDouble() * 2.5;
                  Location var16 = var1.clone().add(Math.cos(var12) * var14, var9.nextDouble() * 0.5, Math.sin(var12) * var14);
                  ParticleBudget.spawnDust(var0, var16, new DustOptions(var6, 1.0F), 1);
               }

               if (var8[0] % 5 == 0) {
                  for (LivingEntity var18 : EntityQueries.living(var0, var1, 2.5)) {
                     if (var18 != var2 && !var18.getType().name().equals("ARMOR_STAND") && !var18.isDead()) {
                        damageEntity(var18, var2, var4 * 0.2, var3);
                     }
                  }
               }

               var8[0]++;
            }
         }, 0L, 1L);
      }
   }

   private static void spawnSphereParticles(World var0, Location var1) {
      ThreadLocalRandom var2 = ThreadLocalRandom.current();
      DustOptions var3 = new DustOptions(Color.fromRGB(180, 255, 120), 1.4F);

      for (int var4 = 0; var4 < 8; var4++) {
         double var5 = var2.nextDouble(Math.PI * 2);
         double var7 = var2.nextDouble(Math.PI);
         double var9 = 1.5 * (0.8 + var2.nextDouble() * 0.2);
         Location var11 = new Location(
            var0,
            var1.getX() + var9 * Math.sin(var7) * Math.cos(var5),
            var1.getY() + var9 * Math.sin(var7) * Math.sin(var5),
            var1.getZ() + var9 * Math.cos(var7)
         );
         var0.spawnParticle(Particle.DUST, var11, 1, 0.0, 0.0, 0.0, 0.0, var3);
         var0.spawnParticle(Particle.NOTE, var11, 1, 0.0, 0.0, 0.0, var2.nextInt(25));
      }
   }

   private static void spawnCircleParticles(World var0, Location var1) {
      ThreadLocalRandom var2 = ThreadLocalRandom.current();
      DustOptions var3 = new DustOptions(Color.fromRGB(255, 215, 0), 1.1F);

      for (int var4 = 0; var4 < 10; var4++) {
         double var5 = var2.nextDouble(Math.PI * 2);
         double var7 = var2.nextDouble() * 6.0;
         Location var9 = new Location(
            var0, var1.getX() + Math.cos(var5) * var7, var1.getY() + (var2.nextDouble() - 0.5) * 1.0 * 2.0, var1.getZ() + Math.sin(var5) * var7
         );
         var0.spawnParticle(Particle.DUST, var9, 1, 0.0, 0.0, 0.0, 0.0, var3);
         if (var4 % 2 == 0) {
            var0.spawnParticle(Particle.NOTE, var9, 1, 0.0, 0.0, 0.0, var2.nextInt(25));
         }
      }
   }

   private static int noteExplosion(World var0, Location var1, Player var2, Map<String, Object> var3, double var4, double var6) {
      ThreadLocalRandom var8 = ThreadLocalRandom.current();
      int var9 = ParticleBudget.request(var0, 24);

      for (int var10 = 0; var10 < var9; var10++) {
         double var11 = var8.nextDouble(Math.PI * 2);
         double var13 = var8.nextDouble() * 5.0;
         var0.spawnParticle(
            Particle.NOTE,
            var1.getX() + Math.cos(var11) * var13,
            var1.getY() + var8.nextDouble() * 2.0,
            var1.getZ() + Math.sin(var11) * var13,
            1,
            0.0,
            0.0,
            0.0,
            var8.nextInt(25)
         );
      }

      int var15 = 0;

      for (LivingEntity var12 : EntityQueries.living(var0, var1, 5.0)) {
         if (var12 != var2 && !var12.getType().name().equals("ARMOR_STAND") && !var12.isDead()) {
            if (var12 instanceof Player) {
               var12.damage(var4, var2);
            } else {
               var12.damage(var4, var2);
               var15 += applyMeritChange(var3, var12.getType().name());
               PotionEffectType var17 = PotionEffectType.SLOWNESS;
               if (var17 != null) {
                  var12.addPotionEffect(new PotionEffect(var17, 40, 1));
               }
            }
         }
      }

      if (var6 > 0.0) {
         safePull(var0, var1, var2, var6);
      }

      var0.playSound(var1, "block.note_block.chime", 0.8F, 1.5F);
      return var15;
   }

   private static int applyAreaDamage(World var0, Location var1, Player var2, double var3, double var5, double var7, double var9, Map<String, Object> var11) {
      int var12 = 0;

      for (LivingEntity var14 : EntityQueries.living(var0, var1, var5, var7, var9)) {
         var12 += processEntityDamage(var14, var2, var3, var11);
      }

      return var12;
   }

   private static int processEntityDamage(LivingEntity var0, Player var1, double var2, Map<String, Object> var4) {
      return var0 != var1 && var0.isValid() && !var0.getType().name().equals("ARMOR_STAND") && !var0.isDead() ? damageEntity(var0, var1, var2, var4) : 0;
   }

   private static int damageEntity(LivingEntity var0, Player var1, double var2, Map<String, Object> var4) {
      if (var0 instanceof Player) {
         var0.damage(var2, var1);
         return 0;
      } else {
         var0.damage(var2, var1);
         int var5 = applyMeritChange(var4, var0.getType().name());
         PotionEffectType var6 = PotionEffectType.BLINDNESS;
         if (var6 != null) {
            var0.addPotionEffect(new PotionEffect(var6, 20, 0));
         }

         return var5;
      }
   }

   private static int applyMeritChange(Map<String, Object> var0, String var1) {
      return ScrollCombatEngine.meritChangeDelta(var0, var1);
   }

   private static void safePull(World var0, Location var1, Player var2, double var3) {
      safePull(var0, var1, var2, var3, 4.0);
   }

   private static void safePull(World var0, Location var1, Player var2, double var3, double var5) {
      for (LivingEntity var8 : EntityQueries.living(var0, var1, var5)) {
         if (var8 != var2 && !var8.getType().name().equals("ARMOR_STAND") && !var8.isDead()) {
            Vector var9 = var1.toVector().subtract(var8.getLocation().toVector());
            if (var9.lengthSquared() > 1.0E-4) {
               var8.setVelocity(var8.getVelocity().add(var9.normalize().multiply(var3)));
            }
         }
      }
   }

   private static Plugin plugin() {
      return Bukkit.getPluginManager().getPlugin("Komutech");
   }
}
