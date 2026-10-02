package tech.komutech.native_scripts.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.ParticleBudget;

public final class ScrollSpecialEffects {
   private static final long CONTINUOUS_EFFECT_INTERVAL = 2L;
   private static final Color CYAN = Color.fromRGB(0, 255, 255);
   private static final Color GREEN = Color.fromRGB(0, 255, 77);

   private ScrollSpecialEffects() {
   }

   public static void execute(String var0, Player var1, Map<String, Object> var2, double var3, double var5, List<Object> var7, List<Object> var8) {
      List var9 = var7.stream().map(String::valueOf).toList();
      int var10 = var8.size() >= 2 ? parseInt(var8.get(0), 1) : 1;
      int var11 = var8.size() >= 2 ? parseInt(var8.get(1), 10) : 10;
      switch (var0) {
         case "勾豆灰":
            gouDouHui(var1, var2, var3, var5);
            break;
         case "碎玉闪":
            suiYuShan(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "九霄环佩鸣":
            jiuXiaoHuanPeiMing(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "寒霜锁":
            hanShuangSuo(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "冰火两重天":
            bingHuoLiangChongTian(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "御龙护身决":
            yuLongHuShenJue(var1, var2, var3, var9, var10, var11);
            break;
         case "游龙惊鸿诀":
            youLongJingHongJue(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "星陨劫":
            xingYunJie(var1, var2, var3, var5, var9, var10, var11);
            break;
         case "五行必杀":
            wuXingBiSha(var1, var2, var3, var9, var10, var11);
            break;
         default:
            defaultRay(var1, var2, var3, var5);
      }
   }

   private static void gouDouHui(Player var0, Map<String, Object> var1, double var2, double var4) {
      Location var6 = var0.getEyeLocation();
      Vector var7 = var6.getDirection();
      Location var8 = var6.clone().add(var7);
      ParticleBudget.spawnRayTrail(var0.getWorld(), var8, var7, var4, CYAN, GREEN, 4.0F, 0.3);
      rayHit(var0, var1, var8, var7, var4, var2);
   }

   private static void suiYuShan(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      World var9 = var0.getWorld();
      Location var10 = var0.getEyeLocation();
      Vector var11 = var10.getDirection().normalize();
      Vector var12 = var11.clone().crossProduct(new Vector(0, 1, 0));
      if (var12.lengthSquared() < 0.01) {
         var12 = var11.clone().crossProduct(new Vector(1, 0, 0));
      }

      Vector var13 = var12.normalize();
      Vector var14 = var13.clone().crossProduct(var11).normalize();
      Color var15 = Color.fromRGB(220, 245, 255);
      Color var16 = Color.fromRGB(255, 215, 120);
      ScrollCombatHelper.MeritTracker var17 = new ScrollCombatHelper.MeritTracker();
      HashSet var18 = new HashSet();
      double var19 = Math.max(8.0, var4);
      byte var21 = 7;

      for (int var22 = 0; var22 < var21; var22++) {
         double var23 = (var22 - (var21 - 1) / 2.0) * 0.18;
         Vector var25 = var11.clone().add(var13.clone().multiply(Math.sin(var23))).add(var14.clone().multiply(Math.sin(var23 * 0.35) * 0.15)).normalize();

         for (double var26 = 0.4; var26 <= var19; var26 += 0.45) {
            Location var28 = var10.clone().add(var25.clone().multiply(var26));
            float var29 = 1.4F + (float)(var26 / var19);
            var9.spawnParticle(Particle.DUST, var28, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(gradient(var15, var16, var26 / var19), var29));
            if ((int)(var26 / 0.45) % 2 == 0) {
               var9.spawnParticle(Particle.END_ROD, var28, 1, 0.02, 0.02, 0.02, 0.01);
            }

            for (LivingEntity var31 : EntityQueries.living(var9, var28, 1.1)) {
               if (var18.add(var31) && ScrollCombatHelper.isValidTarget(var0, var31)) {
                  var31.damage(var2, var0);
                  ScrollCombatHelper.recordMeritHit(var31, var6, var17);
               }
            }
         }
      }

      ScrollCombatHelper.applyBatchMerit(var1, var17.whiteHits, var17.normalHits, var7, var8);
      KomutechSupport.send(var0, "§b✦ §f碎玉飞芒·扇面裂空！");
   }

   private static void hanShuangSuo(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      Plugin var9 = plugin();
      if (var9 != null) {
         World var10 = var0.getWorld();
         Location var11 = var0.getLocation().clone().add(0.0, 0.15, 0.0);
         Vector var12 = var0.getLocation().getDirection().clone().setY(0);
         if (var12.lengthSquared() > 0.01) {
            var11.add(var12.normalize().multiply(Math.min(4.0, var4 * 0.25)));
         }

         ScrollCombatHelper.MeritTracker var13 = new ScrollCombatHelper.MeritTracker();
         HashSet var14 = new HashSet();
         double var15 = var2 * 0.55;
         double var17 = Math.max(10.0, var4);
         int[] var19 = new int[]{0};
         Bukkit.getScheduler().runTaskTimer(var9, var14x -> {
            if (!var0.isOnline() || var0.isDead() || var0.getWorld() != var10) {
               var14x.cancel();
            } else if (var19[0] >= 28) {
               var14x.cancel();
               ScrollCombatHelper.applyBatchMerit(var1, var13.whiteHits, var13.normalHits, var7, var8);
               KomutechSupport.send(var0, "§b✦ §3寒霜归寂·锁意消散");
            } else {
               double var15x = 1.2 + (var17 - 1.2) * (var19[0] / 27.0);
               int var17x = Math.max(16, (int)(var15x * 6.0));
               Color var18 = Color.fromRGB(180, 235, 255);
               Color var19x = Color.fromRGB(100, 170, 255);

               for (int var20 = 0; var20 < var17x; var20++) {
                  double var21 = (Math.PI * 2) * var20 / var17x + var19[0] * 0.08;
                  Location var23 = var11.clone().add(Math.cos(var21) * var15x, 0.05, Math.sin(var21) * var15x);
                  var10.spawnParticle(Particle.DUST, var23, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(gradient(var18, var19x, (double)var20 / var17x), 1.8F));
                  if (var20 % 3 == 0) {
                     var10.spawnParticle(Particle.SNOWFLAKE, var23.clone().add(0.0, 0.3, 0.0), 1, 0.05, 0.15, 0.05, 0.01);
                  }
               }

               if (var19[0] % 2 == 0) {
                  for (int var24 = 0; var24 < 8; var24++) {
                     double var26 = (Math.PI * 2) * var24 / 8.0;
                     Location var28 = var11.clone().add(Math.cos(var26) * (var15x * 0.55), 0.2, Math.sin(var26) * (var15x * 0.55));
                     var10.spawnParticle(Particle.DUST, var28, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var18, 1.35F));
                  }
               }

               if (var19[0] % 3 == 0) {
                  for (LivingEntity var27 : EntityQueries.living(var10, var11, var15x + 0.8)) {
                     if (ScrollCombatHelper.isValidTarget(var0, var27)) {
                        PotionEffectType var22 = PotionEffectType.SLOWNESS;
                        PotionEffectType var29 = PotionEffectType.MINING_FATIGUE;
                        if (var22 != null) {
                           var27.addPotionEffect(new PotionEffect(var22, 80, 2));
                        }

                        if (var29 != null) {
                           var27.addPotionEffect(new PotionEffect(var29, 60, 1));
                        }

                        if (var14.add(var27)) {
                           if (!CombatHitGate.damage(var0, var27, var15, 20)) {
                              var14.remove(var27);
                           } else {
                              ScrollCombatHelper.recordMeritHit(var27, var6, var13);
                           }
                        }
                     }
                  }
               }

               var19[0]++;
            }
         }, 0L, 1L);
         KomutechSupport.send(var0, "§b✦ §3寒霜锁域·步履难移！");
      }
   }

   private static void xingYunJie(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      Plugin var9 = plugin();
      if (var9 != null) {
         World var10 = var0.getWorld();
         Location var11 = var0.getEyeLocation();
         Location var12 = var0.getLocation();
         double var13 = Math.max(12.0, var4);
         Vector var15 = var11.getDirection();
         Location var16 = var11.clone().add(var15);
         RayTraceResult var17 = var10.rayTrace(
            var16,
            var15,
            var13,
            FluidCollisionMode.NEVER,
            true,
            0.4,
            var1x -> var1x != var0 && var1x instanceof LivingEntity var2x && ScrollCombatHelper.isValidTarget(var0, var2x)
         );
         LivingEntity var18 = var17 != null && var17.getHitEntity() instanceof LivingEntity var19 ? var19 : null;
         ArrayList<LivingEntity> var28 = new ArrayList<>();

         for (LivingEntity var21 : EntityQueries.living(var10, var12, var13)) {
            if (ScrollCombatHelper.isValidTarget(var0, var21)) {
               var28.add(var21);
            }
         }

         var28.sort((var1x, var2x) -> Double.compare(var1x.getLocation().distanceSquared(var12), var2x.getLocation().distanceSquared(var12)));
         LivingEntity[] var30 = new LivingEntity[5];
         Location[] var31 = new Location[5];
         int var22 = 0;
         if (var18 != null) {
            var30[var22++] = var18;
         }

         for (LivingEntity var24 : var28) {
            if (var22 >= 5) {
               break;
            }

            if (var24 != var18) {
               var30[var22++] = var24;
            }
         }

         for (int var32 = 0; var32 < 5; var32++) {
            if (var30[var32] == null && var22 > 0) {
               var30[var32] = var30[var32 % var22];
            }

            double var34 = (Math.PI * 2) * var32 / 5.0 + ThreadLocalRandom.current().nextDouble(0.15);
            double var26 = 3.5 + var32 % 3 * 1.8;
            var31[var32] = xingYunFallbackImpact(var10, var12, Math.cos(var34) * var26, Math.sin(var34) * var26);
         }

         ScrollCombatHelper.MeritTracker var33 = new ScrollCombatHelper.MeritTracker();
         int[] var35 = new int[]{0};
         Bukkit.getScheduler().runTaskTimer(var9, var13x -> {
            try {
               if (!var0.isOnline() || var0.isDead() || var0.getWorld() != var10) {
                  var13x.cancel();
                  return;
               }

               if (var35[0] >= 44) {
                  var13x.cancel();
                  ScrollCombatHelper.applyBatchMerit(var1, var33.whiteHits, var33.normalHits, var7, var8);
                  KomutechSupport.send(var0, "§6✦ §e星火燃尽·天劫消散");
                  return;
               }

               Color var14 = Color.fromRGB(255, 230, 120);
               Color var15x = Color.fromRGB(255, 80, 20);

               for (int var16x = 0; var16x < 5; var16x++) {
                  int var17x = var16x * 2;
                  if (var35[0] >= var17x) {
                     int var18x = var35[0] - var17x;
                     Location var19x = resolveXingYunImpact(var10, var30[var16x], var31[var16x]);
                     if (var18x > 16) {
                        if (var18x % 2 == 0 && var18x <= 36) {
                           spawnForcedDust(var10, var19x.clone().add(0.0, 0.2, 0.0), 3, 0.35, 0.1, 0.35, new DustOptions(var15x, 1.5F));
                           spawnForced(var10, Particle.FLAME, var19x, 1, 0.2, 0.05, 0.2, 0.01);
                        }
                     } else {
                        double var20 = var18x / 16.0;
                        Location var22x = var19x.clone().add(0.0, 14.0, 0.0);
                        Location var23 = var22x.clone().add(0.0, -(14.0 * var20), 0.0);
                        spawnForcedDust(var10, var23, 2, 0.05, 0.05, 0.05, new DustOptions(gradient(var14, var15x, var20), 2.4F));
                        spawnForced(var10, Particle.FLAME, var23, 2, 0.08, 0.08, 0.08, 0.01);
                        spawnForced(var10, Particle.END_ROD, var23, 1, 0.02, 0.02, 0.02, 0.02);
                        if (var18x == 16) {
                           spawnForced(var10, Particle.EXPLOSION, var19x, 1, 0.0, 0.0, 0.0, 0.0);
                           spawnForcedDust(var10, var19x, 18, 0.7, 0.2, 0.7, new DustOptions(var15x, 2.0F));

                           for (LivingEntity var25 : EntityQueries.living(var10, var19x, 4.0)) {
                              if (CombatHitGate.damage(var0, var25, var2, 6)) {
                                 var25.setFireTicks(80);
                                 ScrollCombatHelper.recordMeritHit(var25, var6, var33);
                              }
                           }

                           LivingEntity var27 = var30[var16x];
                           if (var27 != null && var27.getLocation().distanceSquared(var19x) <= 64.0 && CombatHitGate.damage(var0, var27, var2, 6)) {
                              var27.setFireTicks(80);
                              ScrollCombatHelper.recordMeritHit(var27, var6, var33);
                           }
                        }
                     }
                  }
               }

               var35[0]++;
            } catch (Throwable var26x) {
               var13x.cancel();
               var9.getLogger().warning("[星陨劫] 特效任务异常: " + var26x.getMessage());
               var26x.printStackTrace();
            }
         }, 0L, 1L);
         KomutechSupport.send(var0, var22 > 0 ? "§6✦ §e星陨天劫·锁定 §f" + var22 + " §e目标！" : "§6✦ §e星陨天劫·环天散落！");
      }
   }

   private static Location resolveXingYunImpact(World var0, LivingEntity var1, Location var2) {
      if (var1 != null && !var1.isDead() && var1.isValid()) {
         double var3 = Math.min(2.2, Math.max(0.8, var1.getHeight() * 0.45));
         return var1.getLocation().clone().add(0.0, var3, 0.0);
      } else {
         return var2.clone();
      }
   }

   private static Location xingYunFallbackImpact(World var0, Location var1, double var2, double var4) {
      Location var6 = var1.clone().add(var2, 0.0, var4);
      var6.setY(groundYNear(var0, var6.getX(), var6.getZ(), var1.getY()));
      return var6;
   }

   private static double groundYNear(World var0, double var1, double var3, double var5) {
      int var7 = (int)Math.floor(var1);
      int var8 = (int)Math.floor(var3);
      int var9 = Math.min(var0.getMaxHeight() - 1, (int)Math.ceil(var5 + 8.0));
      int var10 = Math.max(var0.getMinHeight(), (int)Math.floor(var5 - 32.0));

      for (int var11 = var9; var11 >= var10; var11--) {
         if (var0.getBlockAt(var7, var11, var8).getType().isSolid()) {
            return var11 + 1.0;
         }
      }

      return var5;
   }

   private static void spawnForced(World var0, Particle var1, Location var2, int var3, double var4, double var6, double var8, double var10) {
      var0.spawnParticle(var1, var2, var3, var4, var6, var8, var10);
   }

   private static void spawnForcedDust(World var0, Location var1, int var2, double var3, double var5, double var7, DustOptions var9) {
      var0.spawnParticle(Particle.DUST, var1, var2, var3, var5, var7, 0.0, var9);
   }

   private static void defaultRay(Player var0, Map<String, Object> var1, double var2, double var4) {
      Location var6 = var0.getEyeLocation();
      Vector var7 = var6.getDirection();
      Location var8 = var6.clone().add(var7);
      ParticleBudget.spawnRayTrail(var0.getWorld(), var8, var7, var4, CYAN, GREEN, 4.0F, 0.3);
      rayHit(var0, var1, var8, var7, var4, var2);
   }

   private static void rayHit(Player var0, Map<String, Object> var1, Location var2, Vector var3, double var4, double var6) {
      RayTraceResult var8 = var0.getWorld()
         .rayTrace(
            var2,
            var3,
            var4,
            FluidCollisionMode.NEVER,
            false,
            0.1,
            var1x -> var1x != var0 && var1x instanceof LivingEntity var2x && !var2x.getType().name().equals("ARMOR_STAND")
         );
      if (var8 != null && var8.getHitEntity() instanceof LivingEntity var9) {
         ScrollCombatHelper.damageTarget(var0, var1, var9, var6);
      }
   }

   private static void jiuXiaoHuanPeiMing(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      Location var9 = var0.getEyeLocation();
      Vector var10 = var9.getDirection();
      Location var11 = var9.clone().add(var10);
      World var12 = var0.getWorld();
      ScrollCombatHelper.MeritTracker var13 = new ScrollCombatHelper.MeritTracker();
      HashSet var14 = new HashSet();
      double var15 = Math.max(1.0, var4 / 32.0);

      for (double var17 = 0.0; var17 <= var4; var17 += var15) {
         Location var19 = var11.clone().add(var10.clone().multiply(var17));
         if (ParticleBudget.request(var12, 1) > 0) {
            var12.spawnParticle(Particle.SONIC_BOOM, var19, 1, 0.0, 0.0, 0.0, 0.0);
         }

         for (LivingEntity var21 : EntityQueries.living(var12, var19, 2.0)) {
            if (var14.add(var21) && ScrollCombatHelper.isValidTarget(var0, var21)) {
               var21.damage(var2, var0);
               PotionEffectType var22 = PotionEffectType.SLOWNESS;
               PotionEffectType var23 = PotionEffectType.NAUSEA;
               if (var22 != null) {
                  var21.addPotionEffect(new PotionEffect(var22, 100, 1));
               }

               if (var23 != null) {
                  var21.addPotionEffect(new PotionEffect(var23, 100, 1));
               }

               ScrollCombatHelper.recordMeritHit(var21, var6, var13);
            }
         }
      }

      ScrollCombatHelper.applyBatchMerit(var1, var13.whiteHits, var13.normalHits, var7, var8);
      KomutechSupport.send(var0, "§a九霄环佩，音波激荡！");
   }

   private static void bingHuoLiangChongTian(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      Location var9 = var0.getEyeLocation();
      Vector var10 = var9.getDirection();
      Location var11 = var9.clone().add(var10);
      World var12 = var0.getWorld();
      Vector var13 = new Vector(0, 1, 0);
      Vector var14 = var10.clone().crossProduct(var13);
      if (var14.lengthSquared() < 0.01) {
         var14 = var10.clone().crossProduct(new Vector(1, 0, 0));
      }

      var14.normalize();
      Vector var15 = var14.clone().crossProduct(var10).normalize();
      Color var16 = Color.fromRGB(255, 140, 0);
      Color var17 = Color.fromRGB(255, 69, 0);
      Color var18 = Color.fromRGB(173, 216, 230);
      Color var19 = Color.fromRGB(255, 255, 255);
      ScrollCombatHelper.MeritTracker var20 = new ScrollCombatHelper.MeritTracker();
      HashSet var21 = new HashSet();
      double var22 = 0.55;

      for (double var24 = 0.0; var24 <= var4; var24 += var22) {
         Location var26 = var11.clone().add(var10.clone().multiply(var24));
         double var27 = var24 / var4;
         double var29 = 0.8 * (0.5 + var27 * 1.5);
         double var31 = var24 * 0.8;
         double var33 = var24 * 1.5;
         Location var35 = var26.clone().add(var14.clone().multiply(Math.sin(var31) * var29)).add(var15.clone().multiply(Math.cos(var31) * var29));
         Location var36 = var26.clone()
            .add(var14.clone().multiply(Math.sin(var31 + (Math.PI / 2)) * var29 * 0.8))
            .add(var15.clone().multiply(Math.cos(var31 + (Math.PI / 2)) * var29 * 0.8));
         DustTransition var37 = new DustTransition(var16, var17, 2.2F);
         DustTransition var38 = new DustTransition(var18, var19, 2.2F);
         var12.spawnParticle(Particle.DUST_COLOR_TRANSITION, var35, 1, 0.05, 0.05, 0.05, 0.0, var37);
         var12.spawnParticle(Particle.DUST_COLOR_TRANSITION, var36, 1, 0.05, 0.05, 0.05, 0.0, var38);
         if ((int)(var24 / var22) % 2 == 0) {
            var12.spawnParticle(Particle.FLAME, var26.clone().add(var14.clone().multiply(Math.sin(var33) * var29 * 0.5)), 1, 0.05, 0.05, 0.05, 0.05);
            var12.spawnParticle(Particle.SNOWFLAKE, var26.clone().add(var15.clone().multiply(Math.cos(var33 + 0.3) * var29 * 0.5)), 1, 0.05, 0.05, 0.05, 0.05);
         }
      }

      for (double var39 = 0.0; var39 <= var4; var39 += 0.5) {
         Location var40 = var11.clone().add(var10.clone().multiply(var39));

         for (LivingEntity var28 : EntityQueries.living(var12, var40, 1.5)) {
            if (var21.add(var28) && ScrollCombatHelper.isValidTarget(var0, var28)) {
               var28.damage(var2, var0);
               var28.setFireTicks(60);
               PotionEffectType var42 = PotionEffectType.SLOWNESS;
               if (var42 != null) {
                  var28.addPotionEffect(new PotionEffect(var42, 40, 0));
               }

               ScrollCombatHelper.recordMeritHit(var28, var6, var20);
            }
         }
      }

      ScrollCombatHelper.applyBatchMerit(var1, var20.whiteHits, var20.normalHits, var7, var8);
      KomutechSupport.send(var0, "§a冰火交融，螺旋激荡！");
   }

   private static void yuLongHuShenJue(Player var0, Map<String, Object> var1, double var2, List<String> var4, int var5, int var6) {
      Plugin var7 = plugin();
      if (var7 != null) {
         World var8 = var0.getWorld();
         Color[] var9 = new Color[]{Color.fromRGB(240, 230, 140), Color.fromRGB(255, 215, 0), Color.fromRGB(255, 180, 0), Color.fromRGB(255, 140, 0)};
         ScrollCombatHelper.MeritTracker var10 = new ScrollCombatHelper.MeritTracker();
         long[] var11 = new long[]{System.nanoTime()};
         int[] var12 = new int[]{0};
         Bukkit.getScheduler().runTaskTimer(var7, var12x -> {
            if (!var0.isOnline() || var0.isDead() || var0.getWorld() != var8) {
               var12x.cancel();
            } else if (var12[0] >= 240) {
               var12x.cancel();
               ScrollCombatHelper.applyBatchMerit(var1, var10.whiteHits, var10.normalHits, var5, var6);
               KomutechSupport.send(var0, "§7龙魂消散");
            } else {
               double var13 = (System.nanoTime() - var11[0]) / 1.0E9;
               Location var15 = var0.getLocation().clone().add(0.0, 1.0, 0.0);

               for (int var16 = 0; var16 < 5; var16++) {
                  double var17 = (var12[0] + var16 * 0.2) / 240.0;
                  Location var19 = dragonTrailPos(var15, var17, var13);
                  int var20 = Math.min(var9.length - 1, Math.max(0, (int)((var19.getY() - var15.getY()) / 3.0 + 1.0) * 2));
                  if (ParticleBudget.request(var8, 1) > 0) {
                     var8.spawnParticle(Particle.DUST, var19, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var9[var20], 4.0F));
                  }

                  if (var16 == 0 || var16 == 2) {
                     var8.spawnParticle(Particle.DUST, var19, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var9[var20], 3.0F));
                  }
               }

               if (var12[0] % 8 == 0) {
                  for (LivingEntity var22 : EntityQueries.living(var8, var15, 4.0)) {
                     if (CombatHitGate.damage(var0, var22, var2, 8)) {
                        ScrollCombatHelper.recordMeritHit(var22, var4, var10);
                     }
                  }
               }

               var12[0] = (int)(var12[0] + 2L);
            }
         }, 0L, 2L);
         KomutechSupport.send(var0, "§6✦ §e龙魂护体·御敌无形！");
      }
   }

   private static void wuXingBiSha(Player var0, Map<String, Object> var1, double var2, List<String> var4, int var5, int var6) {
      Plugin var7 = plugin();
      if (var7 != null) {
         World var8 = var0.getWorld();
         Location var9 = var0.getEyeLocation();
         Vector var10 = var9.getDirection().normalize();
         double var11 = -Math.toRadians(var9.getYaw());
         double var13 = Math.toRadians(var9.getPitch());
         double var15 = Math.cos(var11);
         double var17 = Math.sin(var11);
         double var19 = Math.cos(var13);
         double var21 = Math.sin(var13);
         ScrollSpecialEffects.ElementEffect[] var23 = ScrollSpecialEffects.ElementEffect.all();
         Location[] var24 = new Location[var23.length];

         for (int var25 = 0; var25 < var23.length; var25++) {
            double var26 = 3.5 + var23[var25].depthOffset * 0.6;
            var24[var25] = var9.clone().add(var10.clone().multiply(var26)).add(0.0, 0.5, 0.0);
         }

         ScrollCombatHelper.MeritTracker var30 = new ScrollCombatHelper.MeritTracker();
         boolean[] var31 = new boolean[var23.length];
         boolean[] var27 = new boolean[]{false};
         int[] var28 = new int[]{0};
         int var29 = (var23.length - 1) * 5 + 25;
         Bukkit.getScheduler().runTaskTimer(var7, var23x -> {
            if (var0.isOnline() && !var0.isDead() && var0.getWorld() == var8) {
               if (var28[0] >= 80) {
                  var23x.cancel();
                  ScrollCombatHelper.applyBatchMerit(var1, var30.whiteHits, var30.normalHits, var5, var6);
                  KomutechSupport.send(var0, "§6✦ §e五行归元·化作尘埃");
               } else {
                  for (int var24x = 0; var24x < var23.length; var24x++) {
                     ScrollSpecialEffects.ElementEffect var25x = var23[var24x];
                     int var26x = Math.max(0, var28[0] - var24x * 5);
                     double var27x = Math.min(1.0, var26x / 25.0);
                     int var29x = var25x.totalPoints();
                     int var30x = (int)Math.floor(var29x * var27x);
                     int var31x = 0;

                     for (double[][] var35 : var25x.strokes) {
                        int var37 = var31x + var35.length;
                        int var38 = Math.min(var35.length, Math.max(0, var30x - var31x));

                        for (int var39 = 0; var39 < var38; var39++) {
                           double[] var40 = var35[var39];
                           Vector var41 = rotateLocal(var40[0], var40[1], 0.0, var15, var17, var19, var21);
                           Location var42 = var24[var24x].clone().add(var41);
                           Color var43 = gradient(var25x.start, var25x.end, (double)var39 / Math.max(1, var35.length - 1));
                           var8.spawnParticle(Particle.DUST, var42, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var43, 2.0F));
                           if (ThreadLocalRandom.current().nextDouble() < 0.35) {
                              var8.spawnParticle(var25x.spark, var42, 1, 0.05, 0.05, 0.05, 0.01);
                           }
                        }

                        var31x = var37;
                        if (var37 > var30x) {
                           break;
                        }
                     }

                     if (var26x >= 25 && !var31[var24x]) {
                        var31[var24x] = true;
                        wuXingWaveDamage(var8, var24[var24x], 2.8, var0, var2, var4, var30);
                     }
                  }

                  if (var28[0] >= var29 && !var27[0]) {
                     var27[0] = true;
                     Location var44 = var24[var24.length / 2].clone();
                     wuXingWaveDamage(var8, var44, 5.5, var0, var2, var4, var30);
                  }

                  var28[0]++;
               }
            } else {
               var23x.cancel();
            }
         }, 0L, 1L);
      }
   }

   private static void wuXingWaveDamage(
      World var0, Location var1, double var2, Player var4, double var5, List<String> var7, ScrollCombatHelper.MeritTracker var8
   ) {
      HashSet var9 = new HashSet();

      for (LivingEntity var11 : EntityQueries.living(var0, var1, var2)) {
         if (var9.add(var11) && ScrollCombatHelper.isValidTarget(var4, var11)) {
            var11.damage(var5, var4);
            ScrollCombatHelper.recordMeritHit(var11, var7, var8);
         }
      }
   }

   private static Vector rotateLocal(double var0, double var2, double var4, double var6, double var8, double var10, double var12) {
      double var14 = var2 * var10 - var4 * var12;
      double var16 = var2 * var12 + var4 * var10;
      return new Vector(var0 * var6 + var16 * var8, var14, -var0 * var8 + var16 * var6);
   }

   private static void youLongJingHongJue(Player var0, Map<String, Object> var1, double var2, double var4, List<String> var6, int var7, int var8) {
      Plugin var9 = plugin();
      if (var9 != null) {
         World var10 = var0.getWorld();
         Color[][] var11 = new Color[][]{
            {Color.fromRGB(64, 224, 208), Color.fromRGB(0, 206, 209), Color.fromRGB(72, 209, 204)},
            {Color.fromRGB(135, 206, 250), Color.fromRGB(30, 144, 255), Color.fromRGB(0, 191, 255)},
            {Color.fromRGB(176, 196, 222), Color.fromRGB(70, 130, 180), Color.fromRGB(100, 149, 237)}
         };
         Location var12 = var0.getEyeLocation();
         Vector var13 = var12.getDirection().normalize();
         Vector var14 = var13.clone().crossProduct(new Vector(0, 1, 0));
         if (var14.lengthSquared() < 0.01) {
            var14 = var13.clone().crossProduct(new Vector(1, 0, 0));
         }

         Vector var15 = var14.normalize();
         Vector var16 = var15.clone().crossProduct(var13).normalize();
         Vector var17 = var13.clone();
         ScrollCombatHelper.MeritTracker var18 = new ScrollCombatHelper.MeritTracker();
         int[] var19 = new int[]{0};
         double var20 = Math.max(8.0, var4);
         Bukkit.getScheduler()
            .runTaskTimer(
               var9,
               var17x -> {
                  if (var0.isOnline() && !var0.isDead() && var0.getWorld() == var10) {
                     if (var19[0] >= 40) {
                        var17x.cancel();
                        ScrollCombatHelper.applyBatchMerit(var1, var18.whiteHits, var18.normalHits, var7, var8);
                        KomutechSupport.send(var0, "§b✦ §3游龙归渊·惊鸿一瞥");
                     } else {
                        double var18x = var19[0] / 40.0;
                        double var20x = var18x * var20;
                        Location var22 = var12.clone().add(var17.clone().multiply(var20x));

                        for (int var23 = 0; var23 < 3; var23++) {
                           double var24 = var23 * (Math.PI * 2.0 / 3.0) + var19[0] * 0.35;
                           double var26 = 1.2 + 0.4 * Math.sin(var19[0] * 0.2 + var23);
                           Location var28 = var22.clone()
                              .add(var15.clone().multiply(Math.cos(var24) * var26))
                              .add(var16.clone().multiply(Math.sin(var24) * var26 * 0.7))
                              .add(0.0, 0.4, 0.0);
                           Color[] var29 = var11[var23];

                           for (int var30 = 0; var30 < 4; var30++) {
                              double var31 = var30 * 0.35;
                              Location var33 = var28.clone().subtract(var17.clone().multiply(var31));
                              var10.spawnParticle(
                                 Particle.DUST,
                                 var33,
                                 1,
                                 0.0,
                                 0.0,
                                 0.0,
                                 0.0,
                                 new DustOptions(var29[var30 % var29.length], Math.max(1.2F, 2.4F - var30 * 0.35F))
                              );
                           }

                           if (var19[0] % 2 == 0) {
                              var10.spawnParticle(Particle.SOUL_FIRE_FLAME, var28, 1, 0.05, 0.05, 0.05, 0.01);
                           }
                        }

                        if (var19[0] % 2 == 0) {
                           for (LivingEntity var35 : EntityQueries.living(var10, var22, 2.8)) {
                              if (CombatHitGate.damage(var0, var35, var2, 8)) {
                                 ScrollCombatHelper.recordMeritHit(var35, var6, var18);
                              }
                           }
                        }

                        var19[0] = (int)(var19[0] + 2L);
                     }
                  } else {
                     var17x.cancel();
                  }
               },
               0L,
               2L
            );
         KomutechSupport.send(var0, "§b✦ §3三龙惊鸿·破空而去！");
      }
   }

   private static Location dragonTrailPos(Location var0, double var1, double var3) {
      double var5 = var1 * Math.PI * 8.0 + var3 * 0.1;
      double var7 = Math.sin(var3 * 0.7 + var1 * 3.0) * 0.4 + Math.sin(var3 * 1.3 + var1 * 5.0) * 0.2;
      double var9 = 1.0 + 3.0 * (0.5 + var7 * 0.5);
      double var11 = Math.sin(var3 * 0.5 + var1 * 4.0) * 1.2 + Math.sin(var3 * 0.85 + var1 * 7.0) * 0.6;
      double var13 = var11 + 1.0;
      double var15 = Math.sin(var3 * 0.9 + var1 * 6.0) * 0.2;
      return var0.clone().add(Math.cos(var5) * var9 + var15, var13, Math.sin(var5) * var9 + var15);
   }

   private static Color gradient(Color var0, Color var1, double var2) {
      return Color.fromRGB(
         (int)Math.round(var0.getRed() + (var1.getRed() - var0.getRed()) * var2),
         (int)Math.round(var0.getGreen() + (var1.getGreen() - var0.getGreen()) * var2),
         (int)Math.round(var0.getBlue() + (var1.getBlue() - var0.getBlue()) * var2)
      );
   }

   private static int parseInt(Object var0, int var1) {
      if (var0 instanceof Number var2) {
         return var2.intValue();
      } else {
         try {
            return Integer.parseInt(String.valueOf(var0));
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }

   private static Plugin plugin() {
      return Bukkit.getPluginManager().getPlugin("Komutech");
   }

   private static final class ElementEffect {
      final Particle spark;
      final Color start;
      final Color end;
      final double depthOffset;
      final double[][][] strokes;

      ElementEffect(Particle var1, Color var2, Color var3, double var4, double[][][] var6) {
         this.spark = var1;
         this.start = var2;
         this.end = var3;
         this.depthOffset = var4;
         this.strokes = var6;
      }

      int totalPoints() {
         int var1 = 0;

         for (double[][] var5 : this.strokes) {
            var1 += var5.length;
         }

         return var1;
      }

      static ScrollSpecialEffects.ElementEffect[] all() {
         return new ScrollSpecialEffects.ElementEffect[]{
            new ScrollSpecialEffects.ElementEffect(
               Particle.WAX_OFF,
               Color.fromRGB(245, 240, 220),
               Color.fromRGB(255, 215, 0),
               0.0,
               buildStrokes(
                  line(8.0, 1.0, 1.0, 8.0),
                  line(8.0, 1.0, 15.0, 8.0),
                  line(5.0, 6.0, 12.0, 6.0),
                  line(2.0, 10.0, 14.0, 10.0),
                  line(8.0, 7.0, 8.0, 14.0),
                  line(1.0, 15.0, 15.0, 15.0),
                  line(11.0, 12.0, 9.0, 14.0),
                  line(6.0, 12.0, 7.0, 14.0)
               )
            ),
            new ScrollSpecialEffects.ElementEffect(
               Particle.SCRAPE,
               Color.fromRGB(50, 205, 50),
               Color.fromRGB(34, 139, 34),
               3.0,
               buildStrokes(line(8.0, 1.0, 8.0, 15.0), line(1.0, 5.0, 15.0, 5.0), line(7.0, 6.0, 1.0, 12.0), line(9.0, 6.0, 15.0, 12.0))
            ),
            new ScrollSpecialEffects.ElementEffect(
               Particle.SOUL_FIRE_FLAME,
               Color.fromRGB(30, 144, 255),
               Color.fromRGB(0, 0, 255),
               6.0,
               buildStrokes(
                  line(8.0, 1.0, 8.0, 15.0),
                  line(8.0, 15.0, 5.0, 13.0),
                  line(2.0, 5.0, 7.0, 5.0),
                  line(7.0, 5.0, 2.0, 11.0),
                  line(12.0, 2.0, 9.0, 5.0),
                  line(9.0, 5.0, 14.0, 11.0)
               )
            ),
            new ScrollSpecialEffects.ElementEffect(
               Particle.SMALL_FLAME,
               Color.fromRGB(255, 69, 0),
               Color.fromRGB(255, 0, 0),
               9.0,
               buildStrokes(
                  line(3.0, 3.0, 6.0, 6.0), line(7.0, 1.0, 8.0, 7.0), line(8.0, 7.0, 1.0, 15.0), line(13.0, 3.0, 10.0, 6.0), line(9.0, 8.0, 15.0, 15.0)
               )
            ),
            new ScrollSpecialEffects.ElementEffect(
               Particle.WAX_ON,
               Color.fromRGB(210, 180, 140),
               Color.fromRGB(139, 69, 19),
               12.0,
               buildStrokes(line(2.0, 7.0, 14.0, 7.0), line(1.0, 15.0, 15.0, 15.0), line(8.0, 1.0, 8.0, 15.0))
            )
         };
      }

      private static double[][] line(double var0, double var2, double var4, double var6) {
         double[][] var8 = new double[9][2];

         for (int var9 = 0; var9 <= 8; var9++) {
            double var10 = var9 / 8.0;
            double var12 = var0 + (var4 - var0) * var10;
            double var14 = var2 + (var6 - var2) * var10;
            var8[var9][0] = (7.5 - var12) * 0.2;
            var8[var9][1] = (15.0 - var14) * 0.2;
         }

         return var8;
      }

      @SafeVarargs
      private static double[][][] buildStrokes(double[][]... var0) {
         return var0;
      }
   }
}
