package tech.komutech.native_scripts.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.combat.CombatHitGate;
import tech.komutech.native_scripts.combat.ScrollCombatEngine;
import tech.komutech.native_scripts.combat.ScrollCombatHelper;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class HuazangStaffScript implements NativeScript {
   private static final int PETAL_COUNT = 6;
   private static final double PETAL_SPEED = 1.3;
   private static final int PETAL_LIFETIME = 30;
   private static final double BLOOM_RADIUS = 3.5;
   private static final int BLOOM_DURATION = 36;
   private static final int BLOOM_DAMAGE_INTERVAL = 12;
   private static final double BLOOM_DAMAGE_RATIO = 0.1;
   private static final double INITIAL_HIT_RATIO = 0.65;
   private static final int TARGET_HIT_ICD_TICKS = 16;
   private static final double HEAL_RATIO = 0.008;
   private static final Color[] PETAL_COLORS = new Color[]{
      Color.fromRGB(255, 183, 197), Color.fromRGB(255, 107, 129), Color.fromRGB(255, 71, 87), Color.fromRGB(255, 255, 255)
   };
   private static final Map<UUID, HuazangStaffScript.BloomCounter> ACTIVE_BLOOMS = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         return UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
      } else {
         if ("castScroll".equals(var1) && var2.length >= 3 && var2[0] instanceof Player var3) {
            Map var8 = var2[2] instanceof Map var5 ? PlayerAttributeStore.deepCopy(var5) : null;
            if (var8 != null) {
               ItemStack var9 = var2[1] instanceof ItemStack var10 ? var10 : var3.getInventory().getItemInMainHand();
               String var11 = ScrollCombatEngine.getBoundSkill(var9);
               if (var11 != null) {
                  ScrollCombatEngine.castScroll(var3, var9, var8, var11);
                  PlayerAttributeStore.save(var3.getName(), var8);
               }
            }
         }

         return null;
      }
   }

   private Object handleUse(UseEvents.Context var1) {
      if (var1.hand() != EquipmentSlot.HAND) {
         return null;
      } else {
         Player var2 = var1.player();
         ItemStack var3 = var1.item();
         Map var4 = PlayerAttributeStore.load(var2.getName());
         if (var4 == null) {
            KomutechSupport.send(var2, "§c无法读取属性数据");
            return null;
         } else if (var2.isSneaking()) {
            String var5 = ScrollCombatEngine.getBoundSkill(var3);
            if (var5 == null) {
               KomutechSupport.send(var2, "§c未绑定卷轴");
               return null;
            } else {
               ScrollCombatEngine.castScroll(var2, var3, var4, var5);
               PlayerAttributeStore.save(var2.getName(), var4);
               return null;
            }
         } else {
            if (ScrollCombatEngine.handleStaffRightClick(var2, () -> {
               if (ScrollCombatEngine.tryStaffAttack(var2, var3, var4, "花葬")) {
                  spawnPetals(var2, var4, var3);
                  PlayerAttributeStore.save(var2.getName(), var4);
               }
            })) {
               ScrollCombatEngine.switchScroll(var2, var3);
            }

            return null;
         }
      }
   }

   private static void spawnPetals(Player var0, Map<String, Object> var1, ItemStack var2) {
      World var3 = var0.getWorld();
      double var4 = ScrollCombatEngine.calcStaffDamage(var0, var2, var1);
      Location var6 = var0.getLocation();
      Location var7 = var0.getEyeLocation();
      Vector var8 = var7.getDirection();
      Location var9 = var7.clone().add(var8);
      double var10 = ScrollCombatEngine.staffMaxDistance("花葬");
      RayTraceResult var12 = var3.rayTrace(
         var9, var8, var10, FluidCollisionMode.NEVER, false, 0.1, var1x -> var1x instanceof LivingEntity var2x && ScrollCombatHelper.isValidTarget(var0, var2x)
      );
      LivingEntity var13 = var12 != null && var12.getHitEntity() instanceof LivingEntity var14 && ScrollCombatHelper.isValidTarget(var0, var14) ? var14 : null;
      Collection<LivingEntity> var29 = EntityQueries.living(var3, var6, var10);
      ArrayList<Petal> var30 = new ArrayList<>();

      for (int var16 = 0; var16 < 6; var16++) {
         double var17 = (Math.PI / 3) * var16 + ThreadLocalRandom.current().nextDouble(0.4);
         Location var19 = new Location(
            var3, var6.getX() + Math.cos(var17) * 1.5, var6.getY() + 1.2 + ThreadLocalRandom.current().nextDouble(0.5), var6.getZ() + Math.sin(var17) * 1.5
         );
         Vector var20 = new Vector(Math.cos(var17), 0.12 + ThreadLocalRandom.current().nextDouble(0.18), Math.sin(var17)).normalize();
         LivingEntity var21 = null;
         if (var13 != null && var16 < 3) {
            var21 = var13;
         } else if (!var29.isEmpty()) {
            LivingEntity var22 = null;
            double var23 = Double.MAX_VALUE;

            for (LivingEntity var26 : var29) {
               if (ScrollCombatHelper.isValidTarget(var0, var26)) {
                  double var27 = var19.distanceSquared(var26.getLocation());
                  if (var27 < var23) {
                     var23 = var27;
                     var22 = var26;
                  }
               }
            }

            var21 = var22;
         }

         var30.add(new HuazangStaffScript.Petal(var19, var21, var20));
         var3.spawnParticle(Particle.CHERRY_LEAVES, var19, 6, 0.2, 0.2, 0.2, 0.02);
         var3.spawnParticle(Particle.DUST, var19, 2, 0.1, 0.1, 0.1, 0.0, new DustOptions(PETAL_COLORS[var16 % PETAL_COLORS.length], 1.4F));
      }

      Plugin var31 = Bukkit.getPluginManager().getPlugin("Komutech");
      HashSet var32 = new HashSet();
      int[] var18 = new int[]{0};
      Bukkit.getScheduler().runTaskTimer(var31, var8x -> {
         if (var18[0] < 30 && var0.isOnline() && !var0.isDead()) {
            for (HuazangStaffScript.Petal var10x : var30) {
               if (var10x.alive) {
                  if (var10x.target != null && !var10x.target.isDead()) {
                     Vector var11 = var10x.target.getLocation().add(0.0, 1.0, 0.0).toVector().subtract(var10x.loc.toVector());
                     if (var11.lengthSquared() > 0.01) {
                        var10x.loc.add(var11.normalize().multiply(1.3));
                     }

                     if (var10x.loc.distanceSquared(var10x.target.getLocation().add(0.0, 1.0, 0.0)) < 1.5) {
                        UUID var12x = var10x.target.getUniqueId();
                        if (var32.add(var12x)) {
                           spawnBloom(var3, var10x.loc, var0, var1, var4);
                        }

                        var10x.alive = false;
                     }
                  } else {
                     var10x.loc.add(var10x.outward.clone().multiply(0.7150000000000001));
                     if (var18[0] >= 14) {
                        var10x.alive = false;
                     }
                  }

                  if (var10x.alive) {
                     var3.spawnParticle(Particle.CHERRY_LEAVES, var10x.loc, 2, 0.05, 0.05, 0.05, 0.0);
                     Color var13x = PETAL_COLORS[ThreadLocalRandom.current().nextInt(PETAL_COLORS.length)];
                     var3.spawnParticle(Particle.DUST, var10x.loc, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var13x, 1.2F));
                  }
               }
            }

            var18[0]++;
         } else {
            var8x.cancel();
         }
      }, 0L, 1L);
   }

   private static void spawnBloom(World var0, Location var1, Player var2, Map<String, Object> var3, double var4) {
      UUID var6 = var2.getUniqueId();
      HuazangStaffScript.BloomCounter var7 = ACTIVE_BLOOMS.computeIfAbsent(var6, var0x -> new HuazangStaffScript.BloomCounter());
      var7.count++;
      HashSet var8 = new HashSet();
      Collection<LivingEntity> var9 = EntityQueries.living(var0, var1, 3.5);
      double var10 = 0.0;
      String var12 = var2.getName();

      for (LivingEntity var14 : var9) {
         if (ScrollCombatHelper.isValidTarget(var2, var14)) {
            var8.add(var14);
            if (CombatHitGate.damage(var2, var14, var4 * 0.65, 16)) {
               if (!(var14 instanceof Player)) {
                  ScrollCombatEngine.applyMeritChange(var3, var14.getType().name());
               }

               var10 += var4 * 0.008;
            }
         }
      }

      safePull(var0, var1, var2, 0.1);
      if (var10 > 0.0) {
         healPlayer(var2, var10);
      }

      byte var33 = 3;
      byte var34 = 20;

      for (int var15 = 0; var15 < var33; var15++) {
         double var16 = var15 * Math.PI * 2.0 / var33;

         for (int var18 = 0; var18 < var34; var18++) {
            double var19 = (double)var18 / var34;
            double var21 = var19 * 3.5;
            double var23 = var16 + var19 * Math.PI * 2.0;
            double var25 = var1.getX() + Math.cos(var23) * var21;
            double var27 = var1.getZ() + Math.sin(var23) * var21;
            double var29 = var1.getY() + var19 * 1.2;
            float var31 = (float)(0.6 + var19 * 0.8);
            Color var32 = PETAL_COLORS[ThreadLocalRandom.current().nextInt(PETAL_COLORS.length)];
            var0.spawnParticle(Particle.DUST, var25, var29, var27, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(var32, var31));
         }
      }

      var0.playSound(var1, "block.spore_blossom.break", 0.6F, 1.2F);
      Plugin var35 = Bukkit.getPluginManager().getPlugin("Komutech");
      int[] var36 = new int[]{0};
      Bukkit.getScheduler()
         .runTaskTimer(
            var35,
            var10x -> {
               if (var36[0] < 36 && var2.isOnline() && !var2.isDead()) {
                  for (int var11 = 0; var11 < 6; var11++) {
                     double var12x = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                     double var14x = 3.5 * (0.3 + ThreadLocalRandom.current().nextDouble(0.7));
                     Color var16x = PETAL_COLORS[ThreadLocalRandom.current().nextInt(PETAL_COLORS.length)];
                     var0.spawnParticle(
                        Particle.DUST,
                        var1.getX() + Math.cos(var12x) * var14x,
                        var1.getY() + Math.sin(var36[0] * 0.2 + var11) * 0.5,
                        var1.getZ() + Math.sin(var12x) * var14x,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        new DustOptions(var16x, 0.5F)
                     );
                     if (var11 % 2 == 0) {
                        var0.spawnParticle(
                           Particle.CHERRY_LEAVES,
                           var1.getX() + Math.cos(var12x) * var14x,
                           var1.getY() + 0.3,
                           var1.getZ() + Math.sin(var12x) * var14x,
                           1,
                           0.05,
                           0.05,
                           0.05,
                           0.0
                        );
                     }
                  }

                  if (var36[0] % 12 == 0) {
                     double var17 = 0.0;

                     for (LivingEntity var18x : EntityQueries.living(var0, var1, 3.5)) {
                        if (var8.contains(var18x) && ScrollCombatHelper.isValidTarget(var2, var18x)) {
                           double var15x = var4 * 0.1;
                           if (CombatHitGate.damage(var2, var18x, var15x, 16)) {
                              if (!(var18x instanceof Player)) {
                                 ScrollCombatEngine.applyMeritChange(var3, var18x.getType().name());
                              }

                              var17 += var15x * 0.008;
                           }
                        }
                     }

                     if (var17 > 0.0) {
                        healPlayer(var2, var17);
                     }
                  }

                  var36[0]++;
               } else {
                  var7.count--;
                  if (var7.count <= 0) {
                     ACTIVE_BLOOMS.remove(var6);
                     KomutechSupport.send(var2, "§d生命绽放 · 花葬");
                  }

                  var10x.cancel();
               }
            },
            0L,
            1L
         );
   }

   private static void safePull(World var0, Location var1, Player var2, double var3) {
      for (LivingEntity var6 : EntityQueries.living(var0, var1, 3.5)) {
         if (ScrollCombatHelper.isValidTarget(var2, var6)) {
            Vector var7 = var1.toVector().subtract(var6.getLocation().toVector());
            if (var7.lengthSquared() > 1.0E-4) {
               var6.setVelocity(var6.getVelocity().add(var7.normalize().multiply(var3)));
            }
         }
      }
   }

   private static void healPlayer(Player var0, double var1) {
      double var3 = var0.getMaxHealth();
      double var5 = var0.getHealth();
      double var7 = Math.min(var1, var3 - var5);
      if (var7 > 0.0) {
         var0.setHealth(var5 + var7);
      } else {
         var0.setAbsorptionAmount(var0.getAbsorptionAmount() + (var1 - var7));
      }
   }

   private static final class BloomCounter {
      int count;
   }

   private static final class Petal {
      Location loc;
      LivingEntity target;
      final Vector outward;
      boolean alive = true;

      Petal(Location var1, LivingEntity var2, Vector var3) {
         this.loc = var1;
         this.target = var2;
         this.outward = var3;
      }
   }
}
